# Integration tests for Get-SaveLoadAttempt.ps1: the script and its lib\ functions run together, unmocked,
# over logs shaped as 0.98a writes them. Each lib function's own cases are pinned by its unit suite under lib\.
# Run with: Invoke-Pester -Path .\Get-SaveLoadAttempt.IntegrationTests.Tests.ps1
#
# Each fixture under fixtures\ is a hand-written install root holding only starsector-core\starsector.log
# and its rotated siblings. The lines are trimmed copies of what 0.98a writes. No test reads a real log.
# A case that resolves class owners copies its fixture under TestDrive and adds the jars there,
# so no binary file sits in the repository.

BeforeAll {

    . "$PSScriptRoot\helpers\New-TestJar.ps1"

    # Tests\ mirrors the repo, so the script sits at the same relative path under the repo root.
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))

    $script:ScriptPath = Join-Path $repoRoot 'tools\save-migration-tool\Get-SaveLoadAttempt.ps1'
    $script:FixturesRoot = Join-Path $PSScriptRoot 'fixtures'

    function script:Invoke-SaveLoadAttempt {
        <#
            Runs the script in-process, so coverage sees it, with stderr swapped for a buffer and warnings
            collected. The script writes its diagnostics to the console's stderr, outside PowerShell's streams.
        #>
        param([hashtable] $Arguments)

        $stderrBuffer = [System.IO.StringWriter]::new()
        $originalStderr = [Console]::Error

        [Console]::SetError($stderrBuffer)

        try {
            $output = & $script:ScriptPath @Arguments -WarningVariable warnings -WarningAction SilentlyContinue

        } finally {
            [Console]::SetError($originalStderr)
        }

        return [pscustomobject]@{
            Output      = $output
            Warnings    = @($warnings | ForEach-Object { "$_" })
            Diagnostics = @($stderrBuffer.ToString() -split "`r?`n" | Where-Object { $_ })
        }
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

    function script:ConvertTo-ClassEntryName {
        # Names a class's entry as a jar stores it: a.b.C reads as a/b/C.class.
        param([string] $Class)

        return "$($Class.Replace('.', '/')).class"
    }

    function script:Add-ModJar {
        # Writes mods\<id>\mod_info.json declaring one jar, and the jar holding the given classes.
        param(
            [string] $Root,
            [string] $ModId,
            [string[]] $Classes
        )

        $modFolder = Join-Path $Root "mods\$ModId"
        $modInfo = "{ `"id`": `"$ModId`", `"version`": `"1.0`", `"jars`": [`"jars/$ModId.jar`"] }"

        New-Item -ItemType Directory -Path $modFolder -Force | Out-Null
        [System.IO.File]::WriteAllText((Join-Path $modFolder 'mod_info.json'), $modInfo)

        New-TestJar -Path (Join-Path $modFolder "jars\$ModId.jar") `
            -EntryNames @($Classes | ForEach-Object { ConvertTo-ClassEntryName $_ }) | Out-Null
    }
}

Describe 'Get-SaveLoadAttempt.IntegrationTests' {

    Context 'two failed loads of one save, the second split across a log rotation' {

        BeforeAll {

            $script:FixtureRoot = Copy-FixtureRoot 'two-failed-loads'

            # The launcher names a second core jar the install lacks, so the index skips it.
            [System.IO.File]::WriteAllText((Join-Path $script:FixtureRoot 'vmparams'),
                'java.exe -Xms1536m -classpath starfarer_obf.jar;starfarer.api.jar -Dx=y')

            New-TestJar -Path (Join-Path $script:FixtureRoot 'starsector-core\starfarer_obf.jar') -EntryNames @(
                ConvertTo-ClassEntryName 'com.fs.starfarer.loading.specs.HullVariantSpec'
                ConvertTo-ClassEntryName 'com.fs.starfarer.campaign.fleet.FleetMember'
                ConvertTo-ClassEntryName 'com.fs.starfarer.campaign.CampaignEngine'
            ) | Out-Null

            Add-ModJar -Root $script:FixtureRoot -ModId 'aotd_theory_of_toolbox' `
                -Classes @('data.kaysaar.aotd.tot.scripts.submarket.nex.AoTDxNexBlackMarketPlugin')

            # Starlords bundles a copy of a core class, which the index records and the report counts as vanilla.
            Add-ModJar -Root $script:FixtureRoot -ModId 'starlords' `
                -Classes @('starlords.person.Lord', 'com.fs.starfarer.campaign.CampaignEngine')

            [System.IO.File]::WriteAllText((Join-Path $script:FixtureRoot 'mods\enabled_mods.json'),
                '{ "enabledMods": ["aotd_theory_of_toolbox", "starlords"] }')

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
            $chain[4].Owners | Should -Be @('starlords')
            $chain[5].Owners | Should -Be @('vanilla', 'starlords')
        }

        It 'logs what it read and decided to stderr' {

            $logFolder = "$script:FixtureRoot\starsector-core"

            $script:Run.Diagnostics | Should -Be @(
                'Get-SaveLoadAttempt: reading 3 log files, oldest first'
                "Get-SaveLoadAttempt: read $logFolder\starsector.log.2: 44 lines, 3 load attempts start here"
                "Get-SaveLoadAttempt: read $logFolder\starsector.log.1: 31 lines, 1 load attempts start here"
                "Get-SaveLoadAttempt: read $logFolder\starsector.log: 20 lines, 2 load attempts start here"
                ("Get-ModRoster: read 2 mod folders under $script:FixtureRoot\mods and 2 enabled IDs; " +
                    'listing 2 mods (0 unparsed), skipping 0 disabled and 0 superseded copies')
                "Get-SaveLoadAttempt: skipped a declared jar that is not on disk: $logFolder\starfarer.api.jar"
                ('Get-SaveLoadAttempt: indexed classes 5, jars 3, from mods 2 and core classpath entries 2; ' +
                    "classes in more than one owner's jars 1")
                'Get-SaveLoadAttempt: grouped 6 attempts into 2 loads, 2 failed; listing them newest first'
                'Get-SaveLoadAttempt: save_Alpha_1: missing Hull BSC_Buffalo_Mk3, named as BSC_Buffalo_Mk3_default_D'
                ('Get-SaveLoadAttempt: save_Alpha_1: object chain length 6, mod classes 2, nearest ' +
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
            $loads[0].Attempts[0].ObjectChain[5].Owners | Should -Be @('vanilla', 'starlords')
            $run.Warnings.Count | Should -Be 0
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
            [System.IO.File]::WriteAllText((Join-Path $root 'mods\enabled_mods.json'), '{ "enabledMods": ["gone_mod"] }')

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

        It 'skips the class index when no failure names a chain' {

            (Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot }).Diagnostics |
                Should -Contain 'Get-SaveLoadAttempt: no failure names an object chain; skipping the class index'
        }
    }

    Context 'a log with no loads' {

        It 'prints an empty JSON list and says it found nothing' {

            $run = Invoke-SaveLoadAttempt @{ StarsectorRoot = (Get-FixtureRoot 'no-loads'); AsJson = $true }

            $run.Output | Should -Be '[]'
            $run.Diagnostics[-1] |
                Should -Be 'Get-SaveLoadAttempt: grouped 0 attempts into 0 loads, 0 failed; listing them newest first'
        }
    }

    Context 'a missing log' {

        It 'fails naming the path' {

            $missingLog = Join-Path $TestDrive 'starsector.log'

            { Invoke-SaveLoadAttempt @{ LogPath = $missingLog } } | Should -Throw "log not found: $missingLog"
        }
    }
}
