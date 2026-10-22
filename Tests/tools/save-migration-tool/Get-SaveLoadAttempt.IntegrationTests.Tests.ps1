# Integration tests for Get-SaveLoadAttempt.ps1: the script and its lib\ functions run together, unmocked,
# over logs shaped as 0.98a writes them. Each lib function's own cases are pinned by its unit suite under lib\.
# Run with: Invoke-Pester -Path .\Get-SaveLoadAttempt.IntegrationTests.Tests.ps1
#
# Each fixture under fixtures\ is a hand-written install root holding only starsector-core\starsector.log
# and its rotated siblings. The lines are trimmed copies of what 0.98a writes. No test reads a real log.
# A case that resolves class owners copies its fixture under TestDrive and adds the jars there,
# so no binary file sits in the repository.

BeforeAll {

    . "$PSScriptRoot\helpers\Invoke-ScriptCapturingOutput.ps1"
    . "$PSScriptRoot\helpers\New-TestJar.ps1"
    . "$PSScriptRoot\helpers\New-TestMod.ps1"

    # Tests\ mirrors the repo, so the script sits at the same relative path under the repo root.
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))

    $script:ScriptPath = Join-Path $repoRoot 'tools\save-migration-tool\Get-SaveLoadAttempt.ps1'
    $script:FixturesRoot = Join-Path $PSScriptRoot 'fixtures'

    function script:Invoke-SaveLoadAttempt {
        # Runs the script on the given arguments; the loads are the run's Output.
        param([hashtable] $Arguments)

        return Invoke-ScriptCapturingOutput -ScriptPath $script:ScriptPath -Arguments $Arguments
    }

    function script:Format-StableDiagnostics {
        # Blanks the one timing a run logs, so a list of diagnostics compares as text.
        param([string[]] $Diagnostics)

        return $Diagnostics -replace ' in \d+ ms;', ' in <n> ms;'
    }

    function script:Get-FixtureRoot {
        param([string] $Name)

        return Join-Path $script:FixturesRoot $Name
    }

    function script:Copy-FixtureRoot {
        # Copies a fixture's install root under TestDrive and returns the copy.
        param([string] $Name)

        $root = Join-Path $TestDrive $Name

        Copy-Item -LiteralPath (Get-FixtureRoot $Name) -Destination $root -Recurse

        return $root
    }

    function script:Add-ModJar {
        # Writes mods\<id>\mod_info.json declaring one jar, and the jar holding the given classes.
        param(
            [string] $Root,
            [string] $ModId,
            [string[]] $Classes
        )

        $modInfo = "{ `"id`": `"$ModId`", `"version`": `"1.0`", `"jars`": [`"jars/$ModId.jar`"] }"
        $modFolder = New-TestMod -StarsectorRoot $Root -Folder $ModId -ModInfo $modInfo

        New-TestJar -Path (Join-Path $modFolder "jars\$ModId.jar") -ClassNames $Classes | Out-Null
    }
}

Describe 'Get-SaveLoadAttempt.IntegrationTests' {

    Context 'two failed loads of one save, the second split across a log rotation' {

        BeforeAll {

            $script:FixtureRoot = Copy-FixtureRoot 'two-failed-loads'

            # The launcher names a second core jar the install lacks, so the index skips it.
            [System.IO.File]::WriteAllText((Join-Path $script:FixtureRoot 'vmparams'),
                'java.exe -Xms1536m -classpath starfarer_obf.jar;starfarer.api.jar -Dx=y')

            New-TestJar -Path (Join-Path $script:FixtureRoot 'starsector-core\starfarer_obf.jar') -ClassNames @(
                'com.fs.starfarer.loading.specs.HullVariantSpec'
                'com.fs.starfarer.campaign.fleet.FleetMember'
                'com.fs.starfarer.campaign.CampaignEngine'
            ) | Out-Null

            Add-ModJar -Root $script:FixtureRoot -ModId 'aotd_theory_of_toolbox' `
                -Classes @('data.kaysaar.aotd.tot.scripts.submarket.nex.AoTDxNexBlackMarketPlugin')

            # Starlords bundles a copy of a core class, which the game never loads, so vanilla stays its one owner.
            Add-ModJar -Root $script:FixtureRoot -ModId 'starlords' `
                -Classes @('starlords.person.Lord', 'com.fs.starfarer.campaign.CampaignEngine')

            # Nexerelin bundles Starlords' Lord, so the class has two mod owners.
            Add-ModJar -Root $script:FixtureRoot -ModId 'nexerelin' -Classes @('starlords.person.Lord')

            # A jar no zip reader opens is skipped, and the other mods still resolve.
            $brokenFolder = New-TestMod -StarsectorRoot $script:FixtureRoot -Folder 'broken' `
                -ModInfo '{ "id": "broken", "version": "1.0", "jars": ["jars/broken.jar"] }'
            New-Item -ItemType Directory -Path (Join-Path $brokenFolder 'jars') | Out-Null
            [System.IO.File]::WriteAllText((Join-Path $brokenFolder 'jars\broken.jar'), 'not a zip')

            [System.IO.File]::WriteAllText((Join-Path $script:FixtureRoot 'mods\enabled_mods.json'),
                '{ "enabledMods": ["aotd_theory_of_toolbox", "starlords", "nexerelin", "broken"] }')

            $script:Run = Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot }
            $script:Loads = @($script:Run.Output)
        }

        It 'lists each load once, newest first' {

            $script:Loads.Count | Should -Be 2
            $script:Loads[0].Cause | Should -Be 'Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'
            $script:Loads[1].Cause | Should -Be 'Weapon spec [tania_boomerang_launcher] not found!'
        }

        It 'groups the retries under their load, each in the log file and line it starts on' {

            $attempts = $script:Loads[0].Attempts

            $attempts.Source | Should -Be @('campaign', 'campaign.bak', 'campaign.inprogress')
            $attempts.Outcome | Should -Be @('Failed', 'Failed', 'Failed')
            $attempts.LogFile | ForEach-Object { Split-Path -Leaf $_ } |
                Should -Be @('starsector.log.1', 'starsector.log', 'starsector.log')
            $attempts.LogLine | Should -Be @(2, 7, 15)
        }

        It 'reads the root cause past a stack frame of the loader class' {

            $script:Loads[1].CauseType | Should -Be 'java.lang.RuntimeException'
            $script:Loads[0].Attempts[2].CauseType | Should -Be 'java.io.FileNotFoundException'
        }

        It 'reads the missing content of each load and each retry' {

            $script:Loads[0].Missing.Id | Should -Be 'BSC_Buffalo_Mk3'
            $script:Loads[0].Missing.ReportedId | Should -Be 'BSC_Buffalo_Mk3_default_D'
            $script:Loads[0].Attempts[1].Missing.Id | Should -Be 'BSC_Hammerhead_MK2'
            $script:Loads[0].Attempts[2].Missing | Should -BeNullOrEmpty
            $script:Loads[1].Missing.Kind | Should -Be 'Weapon'
        }

        It 'names the save folder and resolves its path' {

            $script:Loads[1].SaveFolder | Should -Be 'save_Alpha_1'
            $script:Loads[1].SavePath | Should -Be 'C:\Games\Starsector\saves\save_Alpha_1'
        }

        It 'reads the object chain from the first debugging block, innermost first' {

            $script:Loads[0].ObjectChain.Depth | Should -Be @(0, 1, 2, 3, 4, 5)
            $script:Loads[0].ObjectChain.Class | Should -Be @(
                'com.fs.starfarer.loading.specs.HullVariantSpec'
                'com.fs.starfarer.campaign.fleet.FleetMember'
                'java.util.ArrayList'
                'data.kaysaar.aotd.tot.scripts.submarket.nex.AoTDxNexBlackMarketPlugin'
                'starlords.person.Lord'
                'com.fs.starfarer.campaign.CampaignEngine'
            )
        }

        It 'names the owners of each class in the chain: vanilla, a mod, several, or none' {

            $chain = $script:Loads[0].ObjectChain

            $chain[0].Owners | Should -Be @('vanilla')
            @($chain[2].Owners).Count | Should -Be 0
            $chain[3].Owners | Should -Be @('aotd_theory_of_toolbox')
            $chain[4].Owners | Should -Be @('nexerelin', 'starlords')
            $chain[5].Owners | Should -Be @('vanilla')
        }

        It 'skips a jar no zip reader opens, naming it and the reason' {

            $brokenJar = "$script:FixtureRoot\mods\broken\jars\broken.jar"

            @($script:Run.Diagnostics -like "Get-SaveLoadAttempt: skipped JAR ${brokenJar}: ?*").Count | Should -Be 1
        }

        It 'logs what it read and decided to stderr' {

            $logFolder = "$script:FixtureRoot\starsector-core"
            $diagnostics = Format-StableDiagnostics $script:Run.Diagnostics |
                Where-Object { $_ -notlike '*broken.jar*' }

            $diagnostics | Should -Be @(
                'Get-SaveLoadAttempt: reading 3 log files, oldest first'
                "Get-SaveLoadAttempt: read $logFolder\starsector.log.2: 44 lines, 3 load attempts start here"
                "Get-SaveLoadAttempt: read $logFolder\starsector.log.1: 31 lines, 1 load attempts start here"
                "Get-SaveLoadAttempt: read $logFolder\starsector.log: 20 lines, 2 load attempts start here"
                'Get-SaveLoadAttempt: grouped 6 attempts into 2 loads, 2 failed; listing them newest first'
                ('Get-SaveLoadAttempt: resolving the owners of 7 object chain classes ' +
                    "from the install at $script:FixtureRoot")
                ("Get-ModRoster: read 4 mod folders under $script:FixtureRoot\mods and 4 enabled IDs; " +
                    'listing 4 mods (0 unparsed), skipping 0 disabled and 0 superseded copies')
                "Get-SaveLoadAttempt: skipped JAR $logFolder\starfarer.api.jar: not on disk"
                ('Get-SaveLoadAttempt: read 4 JARs of 4 mods and 2 core classpath entries in <n> ms; ' +
                    'owners found for 5 classes, more than one mod for 1, mod copies of core classes dropped for 1')
                'Get-SaveLoadAttempt: save_Alpha_1: missing Hull BSC_Buffalo_Mk3, named as BSC_Buffalo_Mk3_default_D'
                'Get-SaveLoadAttempt: save_Alpha_1: object chain length 6, mod classes 2,'
                ((' ' * 'Get-SaveLoadAttempt: '.Length) + '  nearest ' +
                    'data.kaysaar.aotd.tot.scripts.submarket.nex.AoTDxNexBlackMarketPlugin (aotd_theory_of_toolbox) ' +
                    'at depth 3')
                'Get-SaveLoadAttempt: save_Alpha_1: missing Weapon tania_boomerang_launcher'
                'Get-SaveLoadAttempt: save_Alpha_1: object chain length 1, mod classes 0'
            )
        }

        It 'prints the same loads as JSON with -AsJson, the missing content and chain of every attempt included' {

            $run = Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot; AsJson = $true }
            $loads = @(($run.Output -join "`n") | ConvertFrom-Json)

            $loads.Count | Should -Be 2
            $loads[0].Attempts[1].Source | Should -Be 'campaign.bak'
            $loads[0].Attempts[1].LogLine | Should -Be 7
            $loads[0].Attempts[1].Missing.Id | Should -Be 'BSC_Hammerhead_MK2'
            $loads[0].Missing.ReportedId | Should -Be 'BSC_Buffalo_Mk3_default_D'
            $loads[0].Attempts[0].ObjectChain[4].Owners | Should -Be @('nexerelin', 'starlords')

            $run.Warnings.Count | Should -Be 0
        }

        It 'lists only the newest failed load with -Outcome Failed -Newest, resolving owners for its chain alone' {

            $run = Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot; Outcome = 'Failed'; Newest = $true }

            @($run.Output).Cause | Should -Be @('Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!')

            $run.Diagnostics | Should -Contain (
                'Get-SaveLoadAttempt: grouped 6 attempts into 2 loads, 2 failed; ' +
                'listing 1 newest first: ending Failed, the newest only')

            $run.Diagnostics | Should -Contain (
                'Get-SaveLoadAttempt: resolving the owners of 6 object chain classes ' +
                "from the install at $script:FixtureRoot")

            @($run.Diagnostics -like '*save_Alpha_1: missing*').Count | Should -Be 1
        }

        It 'lists only the loads of the save named by -SaveFolder, resolving no owners when there are none' {

            $run = Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot; SaveFolder = 'save_Other_9' }

            @($run.Output).Count | Should -Be 0
            $run.Diagnostics | Should -Contain (
                'Get-SaveLoadAttempt: no listed failure names an object chain; resolving no class owners')
        }

        It 'writes the newest failure as text with -AsText: its readings, its retries and its mod classes' {

            $run = Invoke-SaveLoadAttempt @{
                StarsectorRoot = $script:FixtureRoot
                Outcome        = 'Failed'
                Newest         = $true
                AsText         = $true
            }

            $run.Output | Should -Be @(
                ''
                'save_Alpha_1: Failed'
                '  Cause:'
                '    java.lang.RuntimeException'
                '    Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'
                '  save_Alpha_1: missing Hull BSC_Buffalo_Mk3, named as BSC_Buffalo_Mk3_default_D'
                '  save_Alpha_1: object chain length 6, mod classes 2,'
                ('    nearest data.kaysaar.aotd.tot.scripts.submarket.nex.AoTDxNexBlackMarketPlugin ' +
                    '(aotd_theory_of_toolbox) at depth 3')
                '  Attempts, in order:'
                '    campaign:'
                '      Failed at starsector.log.1:2'
                '      java.lang.RuntimeException'
                '      Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'
                '    campaign.bak:'
                '      Failed at starsector.log:7'
                '      java.lang.RuntimeException'
                '      Ship hull spec [BSC_Hammerhead_MK2_default_D] not found!'
                '    campaign.inprogress:'
                '      Failed at starsector.log:15'
                '      java.io.FileNotFoundException'
                ('      C:\Games\Starsector\starsector-core\..\saves\save_Alpha_1\campaign.xml.inprogress ' +
                    '(The system cannot find the file specified)')
                '  Mod classes in the object chain, innermost first:'
                '    depth 3: data.kaysaar.aotd.tot.scripts.submarket.nex.AoTDxNexBlackMarketPlugin (aotd_theory_of_toolbox)'
                '    depth 4: starlords.person.Lord (nexerelin, starlords)'
            )
        }

        It 'refuses -AsJson and -AsText together' {

            { Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot; AsJson = $true; AsText = $true } } |
                Should -Throw '-AsJson and -AsText each choose the output; pass one'
        }

        It 'reads the same loads from a log named by -LogPath' {

            $run = Invoke-SaveLoadAttempt @{ LogPath = "$script:FixtureRoot\starsector-core\starsector.log" }

            @($run.Output).Cause | Should -Be @(
                'Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'
                'Weapon spec [tania_boomerang_launcher] not found!'
            )
        }
    }

    Context 'a load that failed on the save and loaded from the bak' {

        BeforeAll {

            $script:FixtureRoot = Get-FixtureRoot 'recovered-from-bak'
            $script:Run = Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot }
            $script:Loads = @($script:Run.Output)
        }

        It 'reports the load as loaded, with the cause the save failed on' {

            $script:Loads.Count | Should -Be 1
            $script:Loads[0].Outcome | Should -Be 'Loaded'
            $script:Loads[0].Attempts.Source | Should -Be @('campaign', 'campaign.bak')
            $script:Loads[0].CauseType | Should -Be 'com.thoughtworks.xstream.mapper.CannotResolveClassException'
        }

        It 'reads the unresolved class as missing content' {

            $script:Loads[0].Missing.Kind | Should -Be 'Class'
            $script:Loads[0].Missing.Id | Should -Be 'data.scripts.removed.GoneScript'
        }

        It 'reads the chain, but warns and names no owners when the install holds no mods folder' {

            $script:Loads[0].ObjectChain.Class | Should -Be @('data.scripts.removed.GoneScript')
            @($script:Loads[0].ObjectChain[0].Owners).Count | Should -Be 0
            $script:Run.Warnings | Should -Be @(
                "no mods folder in the log's install, $script:FixtureRoot; object chain classes stay without owners"
            )
        }
    }

    Context 'an install whose vmparams names no classpath' {

        It 'indexes the mods, and warns that vanilla classes stay without owners' {

            $root = Copy-FixtureRoot 'recovered-from-bak'

            Add-ModJar -Root $root -ModId 'gone_mod' -Classes @('data.scripts.removed.GoneScript')
            [System.IO.File]::WriteAllText((Join-Path $root 'mods\enabled_mods.json'),
                '{ "enabledMods": ["gone_mod"] }')

            $run = Invoke-SaveLoadAttempt @{ StarsectorRoot = $root }

            @($run.Output)[0].ObjectChain[0].Owners | Should -Be @('gone_mod')
            $run.Warnings | Should -Be @("vmparams in $root names no classpath; vanilla classes stay without owners")
        }
    }

    Context 'a clean load, then a load the log ends inside' {

        BeforeAll {

            $script:FixtureRoot = Get-FixtureRoot 'loaded-then-unfinished'
            $script:Loads = @((Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot }).Output)
        }

        It 'reports the cut-off load first, as unfinished' {

            $script:Loads[0].SaveFolder | Should -Be 'save_Gamma_3'
            $script:Loads[0].Outcome | Should -Be 'Unfinished'
        }

        It 'reports the clean load with no cause, its relative path resolved against the log folder' {

            $script:Loads[1].Outcome | Should -Be 'Loaded'
            $script:Loads[1].Cause | Should -BeNullOrEmpty
            $script:Loads[1].Missing | Should -BeNullOrEmpty
            $script:Loads[1].ObjectChain.Count | Should -Be 0
            $script:Loads[1].SavePath | Should -Be "$script:FixtureRoot\saves\save_Beta_2"
        }

        It 'resolves no class owners when no failure names a chain' {

            (Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot }).Diagnostics |
                Should -Contain 'Get-SaveLoadAttempt: no listed failure names an object chain; resolving no class owners'
        }
    }

    Context 'a log with no loads' {

        It 'prints an empty JSON list and says it found nothing' {

            $run = Invoke-SaveLoadAttempt @{ StarsectorRoot = (Get-FixtureRoot 'no-loads'); AsJson = $true }

            $run.Output | Should -Be '[]'
            $run.Diagnostics |
                Should -Contain 'Get-SaveLoadAttempt: grouped 0 attempts into 0 loads, 0 failed; listing them newest first'
        }
    }

    Context 'a missing log' {

        It 'fails naming the path' {

            $missingLog = Join-Path $TestDrive 'starsector.log'

            { Invoke-SaveLoadAttempt @{ LogPath = $missingLog } } | Should -Throw "log not found: $missingLog"
        }
    }
}
