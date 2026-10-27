# Integration tests for Get-SaveLoadAttempt.ps1: the script and its lib\ functions run together, unmocked,
# over logs shaped as 0.98a writes them. Each lib function's own cases are pinned by its unit suite under lib\.
# Run with: Invoke-Pester -Path .\Get-SaveLoadAttempt.IntegrationTests.Tests.ps1
#
# Each fixture under fixtures\ is a hand-written install root holding only starsector-core\starsector.log
# and its rotated siblings. The lines are trimmed copies of what 0.98a writes. No test reads a real log.
# A case that resolves class owners copies its fixture under TestDrive and adds the JARs there,
# so no binary file sits in the repository.
# A context sharing one run makes it in BeforeAll; each case's act then reads its part of that run.

BeforeAll {

    Set-StrictMode -Version Latest

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

        return $Diagnostics -replace ' in \d+ ms', ' in <n> ms'
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
        # Writes `mods\<id>\mod_info.json` declaring one JAR, and the JAR holding the given classes.
        param(
            [string] $Root,
            [string] $ModId,
            [string[]] $Classes
        )

        $modInfo = "{ `"id`": `"$ModId`", `"name`": `"$ModId`", `"description`": `"`", `"version`": `"1.0`", " +
            "`"jars`": [`"jars/$ModId.jar`"] }"
        $modFolder = New-TestMod -StarsectorRoot $Root -Folder $ModId -ModInfo $modInfo

        New-TestJar -Path (Join-Path $modFolder "jars\$ModId.jar") -ClassNames $Classes | Out-Null
    }
}

Describe 'Get-SaveLoadAttempt.IntegrationTests' {

    Context 'two failed loads of one save, the second split across a log rotation' {

        BeforeAll {

            $script:FixtureRoot = Copy-FixtureRoot 'two-failed-loads'

            # The launcher names a second core JAR the install lacks, so the index skips it.
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

            # A JAR no zip reader opens is skipped, and the other mods still resolve.
            $brokenFolder = New-TestMod -StarsectorRoot $script:FixtureRoot -Folder 'broken' `
                -ModInfo ('{ "id": "broken", "name": "Broken", "description": "", "version": "1.0", ' +
                    '"jars": ["jars/broken.jar"] }')
            New-Item -ItemType Directory -Path (Join-Path $brokenFolder 'jars') | Out-Null
            [System.IO.File]::WriteAllText((Join-Path $brokenFolder 'jars\broken.jar'), 'not a zip')

            [System.IO.File]::WriteAllText((Join-Path $script:FixtureRoot 'mods\enabled_mods.json'),
                '{ "enabledMods": ["aotd_theory_of_toolbox", "starlords", "nexerelin", "broken"] }')

            $script:Run = Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot }
        }

        It 'lists each load once, newest first' {

            # ARRANGE
            $expectedCauses = @(
                'Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'
                'Weapon spec [tania_boomerang_launcher] not found!'
            )

            # ACT
            $actualLoads = @($script:Run.Output)

            # ASSERT
            $actualLoads.Cause | Should -Be $expectedCauses
        }

        It 'groups the retries under their load, each in the log file and line it starts on' {

            # ARRANGE
            $expectedSources = @('campaign', 'campaign.bak', 'campaign.inprogress')
            $expectedOutcomes = @('Failed', 'Failed', 'Failed')
            $expectedLogFiles = @('starsector.log.1', 'starsector.log', 'starsector.log')
            $expectedLogLines = @(2, 7, 15)

            # ACT
            $actualAttempts = @($script:Run.Output)[0].Attempts

            # ASSERT
            $actualAttempts.Source | Should -Be $expectedSources
            $actualAttempts.Outcome | Should -Be $expectedOutcomes
            $actualAttempts.LogFile | ForEach-Object { Split-Path -Leaf $_ } | Should -Be $expectedLogFiles
            $actualAttempts.LogLine | Should -Be $expectedLogLines
        }

        It 'reads the root cause past a stack frame of the loader class' {

            # ARRANGE
            $expectedOlderCauseType = 'java.lang.RuntimeException'
            $expectedRetryCauseType = 'java.io.FileNotFoundException'

            # ACT
            $actualLoads = @($script:Run.Output)

            # ASSERT
            $actualLoads[1].CauseType | Should -Be $expectedOlderCauseType
            $actualLoads[0].Attempts[2].CauseType | Should -Be $expectedRetryCauseType
        }

        It 'reads the missing content of each load and each retry' {

            # ARRANGE
            $expectedNewestId = 'BSC_Buffalo_Mk3'
            $expectedNewestReportedId = 'BSC_Buffalo_Mk3_default_D'
            $expectedRetryId = 'BSC_Hammerhead_MK2'
            $expectedOlderKind = 'Weapon'

            # ACT
            $actualLoads = @($script:Run.Output)

            # ASSERT
            $actualLoads[0].Missing.Id | Should -Be $expectedNewestId
            $actualLoads[0].Missing.ReportedId | Should -Be $expectedNewestReportedId
            $actualLoads[0].Attempts[1].Missing.Id | Should -Be $expectedRetryId
            $actualLoads[0].Attempts[2].Missing | Should -BeNullOrEmpty
            $actualLoads[1].Missing.Kind | Should -Be $expectedOlderKind
        }

        It 'names the save folder and resolves its path' {

            # ARRANGE
            $expectedSaveFolder = 'save_Alpha_1'
            $expectedSavePath = 'C:\Games\Starsector\saves\save_Alpha_1'

            # ACT
            $actualLoad = @($script:Run.Output)[1]

            # ASSERT
            $actualLoad.SaveFolder | Should -Be $expectedSaveFolder
            $actualLoad.SavePath | Should -Be $expectedSavePath
        }

        It 'reads the object chain from the first debugging block, innermost first' {

            # ARRANGE
            $expectedDepths = @(0, 1, 2, 3, 4, 5)
            $expectedClasses = @(
                'com.fs.starfarer.loading.specs.HullVariantSpec'
                'com.fs.starfarer.campaign.fleet.FleetMember'
                'java.util.ArrayList'
                'data.kaysaar.aotd.tot.scripts.submarket.nex.AoTDxNexBlackMarketPlugin'
                'starlords.person.Lord'
                'com.fs.starfarer.campaign.CampaignEngine'
            )

            # ACT
            $actualChain = @($script:Run.Output)[0].ObjectChain

            # ASSERT
            $actualChain.Depth | Should -Be $expectedDepths
            $actualChain.Class | Should -Be $expectedClasses
        }

        It 'names the owners of each class in the chain: vanilla, a mod, several, or none' {

            # ARRANGE
            $expectedCoreOwners = @('vanilla')
            $expectedUnownedCount = 0
            $expectedModOwners = @('aotd_theory_of_toolbox')
            $expectedSharedOwners = @('nexerelin', 'starlords')

            # ACT
            $actualChain = @($script:Run.Output)[0].ObjectChain

            # ASSERT
            $actualChain[0].Owners | Should -Be $expectedCoreOwners
            @($actualChain[2].Owners).Count | Should -Be $expectedUnownedCount
            $actualChain[3].Owners | Should -Be $expectedModOwners
            $actualChain[4].Owners | Should -Be $expectedSharedOwners
            $actualChain[5].Owners | Should -Be $expectedCoreOwners
        }

        It 'skips a JAR no zip reader opens, naming it and the reason' {

            # ARRANGE
            $brokenJar = "$script:FixtureRoot\mods\broken\jars\broken.jar"
            $expectedSkipPattern = "Get-SaveLoadAttempt: skipped JAR ${brokenJar}: ?*"
            $expectedSkipCount = 1

            # ACT
            $actualSkipLines = @($script:Run.Diagnostics -like $expectedSkipPattern)

            # ASSERT
            $actualSkipLines.Count | Should -Be $expectedSkipCount
        }

        It 'logs what it read and decided to stderr, blank lines parting scripts and each load''s readings' {

            # ARRANGE
            $logFolder = "$script:FixtureRoot\starsector-core"
            $expectedDiagnostics = @(
                'Get-SaveLoadAttempt: reading 3 log files, oldest first'
                "Get-SaveLoadAttempt: read $logFolder\starsector.log.2: 44 lines, 3 load attempts start here"
                "Get-SaveLoadAttempt: read $logFolder\starsector.log.1: 31 lines, 1 load attempts start here"
                "Get-SaveLoadAttempt: read $logFolder\starsector.log: 20 lines, 2 load attempts start here"
                'Get-SaveLoadAttempt: grouped 6 attempts into 2 loads, 2 failed; listing them newest first'
                ('Get-SaveLoadAttempt: resolving the owners of 7 object chain classes ' +
                    "from the install at $script:FixtureRoot")
                ("Get-SaveLoadAttempt: read 4 mod folders under $script:FixtureRoot\mods and 4 enabled IDs; " +
                    'listing 4 mods (0 unparsed), skipping 0 disabled and 0 superseded copies')
                "Get-SaveLoadAttempt: skipped JAR $logFolder\starfarer.api.jar: not on disk"
                ('Get-SaveLoadAttempt: read 3 JARs of 4 mods and 1 of 2 core classpath entries in <n> ms, ' +
                    'the JARs of 0 unparsed mods unknown; ' +
                    'owners found for 5 classes, more than one mod for 1, mod copies of core classes dropped for 1')
                ''
                'Get-SaveLoadAttempt: save_Alpha_1: missing Hull BSC_Buffalo_Mk3, named as BSC_Buffalo_Mk3_default_D'
                'Get-SaveLoadAttempt: save_Alpha_1: object chain length 6, mod classes 2,'
                ((' ' * 'Get-SaveLoadAttempt: '.Length) + '  nearest ' +
                    'data.kaysaar.aotd.tot.scripts.submarket.nex.AoTDxNexBlackMarketPlugin (aotd_theory_of_toolbox) ' +
                    'at depth 3')
                ''
                'Get-SaveLoadAttempt: save_Alpha_1: missing Weapon tania_boomerang_launcher'
                'Get-SaveLoadAttempt: save_Alpha_1: object chain length 1, mod classes 0'
            )

            # ACT
            $actualDiagnostics = Format-StableDiagnostics $script:Run.Diagnostics |
                Where-Object { $_ -notlike '*broken.jar*' }

            # ASSERT
            $actualDiagnostics | Should -Be $expectedDiagnostics
        }

        It 'prints the same loads as JSON with -AsJson, the missing content and chain of every attempt included' {

            # ARRANGE
            $expectedCount = 2
            $expectedRetrySource = 'campaign.bak'
            $expectedRetryLogLine = 7
            $expectedRetryMissingId = 'BSC_Hammerhead_MK2'
            $expectedReportedId = 'BSC_Buffalo_Mk3_default_D'
            $expectedSharedOwners = @('nexerelin', 'starlords')
            $expectedWarningCount = 0

            # ACT
            $actualRun = Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot; AsJson = $true }

            # ASSERT
            $actualLoads = @(($actualRun.Output -join "`n") | ConvertFrom-Json)

            $actualLoads.Count | Should -Be $expectedCount
            $actualLoads[0].Attempts[1].Source | Should -Be $expectedRetrySource
            $actualLoads[0].Attempts[1].LogLine | Should -Be $expectedRetryLogLine
            $actualLoads[0].Attempts[1].Missing.Id | Should -Be $expectedRetryMissingId
            $actualLoads[0].Missing.ReportedId | Should -Be $expectedReportedId
            $actualLoads[0].Attempts[0].ObjectChain[4].Owners | Should -Be $expectedSharedOwners
            $actualRun.Warnings.Count | Should -Be $expectedWarningCount
        }

        It 'lists only the newest failed load with -Outcome Failed -Newest, resolving owners for its chain alone' {

            # ARRANGE
            $expectedCauses = @('Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!')
            $expectedGroupedLine = 'Get-SaveLoadAttempt: grouped 6 attempts into 2 loads, 2 failed; ' +
                'listing 1 newest first: ending Failed, the newest only'
            $expectedResolvingLine = 'Get-SaveLoadAttempt: resolving the owners of 6 object chain classes ' +
                "from the install at $script:FixtureRoot"
            $expectedReadingCount = 1

            # ACT
            $actualRun = Invoke-SaveLoadAttempt @{
                StarsectorRoot = $script:FixtureRoot
                Outcome        = 'Failed'
                Newest         = $true
            }

            # ASSERT
            @($actualRun.Output).Cause | Should -Be $expectedCauses
            $actualRun.Diagnostics | Should -Contain $expectedGroupedLine
            $actualRun.Diagnostics | Should -Contain $expectedResolvingLine
            @($actualRun.Diagnostics -like '*save_Alpha_1: missing*').Count | Should -Be $expectedReadingCount
        }

        It 'lists only the loads of the save named by -SaveFolder, resolving no owners when there are none' {

            # ARRANGE
            $expectedCount = 0
            $expectedLine = 'Get-SaveLoadAttempt: no listed failure names an object chain; resolving no class owners'

            # ACT
            $actualRun = Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot; SaveFolder = 'save_Other_9' }

            # ASSERT
            $actualRun.Output.Count | Should -Be $expectedCount
            $actualRun.Diagnostics | Should -Contain $expectedLine
        }

        It 'writes the newest failure as text with -AsText: its readings, its retries and its mod classes' {

            # ARRANGE
            $expectedOutput = @(
                ''
                'save_Alpha_1: Failed'
                '  Cause:'
                '    java.lang.RuntimeException'
                '    Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'
                '  missing Hull BSC_Buffalo_Mk3, named as BSC_Buffalo_Mk3_default_D'
                '  object chain length 6, mod classes 2,'
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
                ('    depth 3: data.kaysaar.aotd.tot.scripts.submarket.nex.AoTDxNexBlackMarketPlugin ' +
                    '(aotd_theory_of_toolbox)')
                '    depth 4: starlords.person.Lord (nexerelin, starlords)'
            )

            # ACT
            $actualRun = Invoke-SaveLoadAttempt @{
                StarsectorRoot = $script:FixtureRoot
                Outcome        = 'Failed'
                Newest         = $true
                AsText         = $true
            }

            # ASSERT
            $actualRun.Output | Should -Be $expectedOutput
        }

        # A caller that read the roster for its own use hands it over, so the install's mods are read once.
        It 'resolves owners against a roster given with -Roster, reading no mods folder' {

            # ARRANGE
            $roster = @([pscustomobject]@{ Id = 'given'; Jars = @(); Status = 'parsed' })
            $expectedRosterReadCount = 0
            $expectedJarReadCount = 1

            # ACT
            $actualRun = Invoke-SaveLoadAttempt @{
                StarsectorRoot = $script:FixtureRoot
                Outcome        = 'Failed'
                Newest         = $true
                Roster         = $roster
            }

            # ASSERT
            @($actualRun.Diagnostics -like '*mod folders under*').Count | Should -Be $expectedRosterReadCount
            @($actualRun.Diagnostics -like 'Get-SaveLoadAttempt: read 0 JARs of 1 mods and *').Count |
                Should -Be $expectedJarReadCount
        }

        It 'refuses -AsJson and -AsText together' {

            # ARRANGE
            $expectedMessage = '-AsJson and -AsText each choose the output; pass one'

            # ACT
            $actualError = {
                Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot; AsJson = $true; AsText = $true }
            } | Should -Throw -PassThru

            # ASSERT
            $actualError.Exception.Message | Should -Be $expectedMessage
        }

        It 'reads the same loads from a log named by -LogPath' {

            # ARRANGE
            $expectedCauses = @(
                'Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'
                'Weapon spec [tania_boomerang_launcher] not found!'
            )

            # ACT
            $actualRun = Invoke-SaveLoadAttempt @{ LogPath = "$script:FixtureRoot\starsector-core\starsector.log" }

            # ASSERT
            @($actualRun.Output).Cause | Should -Be $expectedCauses
        }
    }

    Context 'a load that failed on the save and loaded from the bak' {

        BeforeAll {

            $script:FixtureRoot = Get-FixtureRoot 'recovered-from-bak'
            $script:Run = Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot }
        }

        It 'reports the load as loaded, with the cause the save failed on' {

            # ARRANGE
            $expectedCount = 1
            $expectedOutcome = 'Loaded'
            $expectedSources = @('campaign', 'campaign.bak')
            $expectedCauseType = 'com.thoughtworks.xstream.mapper.CannotResolveClassException'

            # ACT
            $actualLoads = @($script:Run.Output)

            # ASSERT
            $actualLoads.Count | Should -Be $expectedCount
            $actualLoads[0].Outcome | Should -Be $expectedOutcome
            $actualLoads[0].Attempts.Source | Should -Be $expectedSources
            $actualLoads[0].CauseType | Should -Be $expectedCauseType
        }

        It 'reads the unresolved class as missing content' {

            # ARRANGE
            $expectedKind = 'Class'
            $expectedId = 'data.scripts.removed.GoneScript'

            # ACT
            $actualMissing = @($script:Run.Output)[0].Missing

            # ASSERT
            $actualMissing.Kind | Should -Be $expectedKind
            $actualMissing.Id | Should -Be $expectedId
        }

        # The game's own classes need no mods folder, so they are still looked up.
        It 'reads the chain, and warns that it lists no mods when the install holds no mods folder' {

            # ARRANGE
            $expectedClasses = @('data.scripts.removed.GoneScript')
            $expectedOwnerCount = 0
            $expectedWarnings = @(
                "mods directory not found, listing no mods: $script:FixtureRoot\mods"
                "$script:FixtureRoot\vmparams not found; vanilla classes stay without owners"
            )

            # ACT
            $actualChain = @($script:Run.Output)[0].ObjectChain

            # ASSERT
            $actualChain.Class | Should -Be $expectedClasses
            @($actualChain[0].Owners).Count | Should -Be $expectedOwnerCount
            $script:Run.Warnings | Should -Be $expectedWarnings
        }
    }

    Context 'an install with no vmparams' {

        It 'indexes the mods, and warns why vanilla classes stay without owners' {

            # ARRANGE
            $root = Copy-FixtureRoot 'recovered-from-bak'

            Add-ModJar -Root $root -ModId 'gone_mod' -Classes @('data.scripts.removed.GoneScript')
            [System.IO.File]::WriteAllText((Join-Path $root 'mods\enabled_mods.json'),
                '{ "enabledMods": ["gone_mod"] }')

            $expectedOwners = @('gone_mod')
            $expectedWarnings = @("$root\vmparams not found; vanilla classes stay without owners")

            # ACT
            $actualRun = Invoke-SaveLoadAttempt @{ StarsectorRoot = $root }

            # ASSERT
            @($actualRun.Output)[0].ObjectChain[0].Owners | Should -Be $expectedOwners
            $actualRun.Warnings | Should -Be $expectedWarnings
        }
    }

    Context 'a clean load, then a load the log ends inside' {

        BeforeAll {

            $script:FixtureRoot = Get-FixtureRoot 'loaded-then-unfinished'
            $script:Run = Invoke-SaveLoadAttempt @{ StarsectorRoot = $script:FixtureRoot }
        }

        It 'reports the cut-off load first, as unfinished' {

            # ARRANGE
            $expectedSaveFolder = 'save_Gamma_3'
            $expectedOutcome = 'Unfinished'

            # ACT
            $actualLoad = @($script:Run.Output)[0]

            # ASSERT
            $actualLoad.SaveFolder | Should -Be $expectedSaveFolder
            $actualLoad.Outcome | Should -Be $expectedOutcome
        }

        It 'reports the clean load with no cause, its relative path resolved against the log folder' {

            # ARRANGE
            $expectedOutcome = 'Loaded'
            $expectedChainCount = 0
            $expectedSavePath = "$script:FixtureRoot\saves\save_Beta_2"

            # ACT
            $actualLoad = @($script:Run.Output)[1]

            # ASSERT
            $actualLoad.Outcome | Should -Be $expectedOutcome
            $actualLoad.Cause | Should -BeNullOrEmpty
            $actualLoad.Missing | Should -BeNullOrEmpty
            $actualLoad.ObjectChain.Count | Should -Be $expectedChainCount
            $actualLoad.SavePath | Should -Be $expectedSavePath
        }

        It 'resolves no class owners when no failure names a chain' {

            # ARRANGE
            $expectedLine = 'Get-SaveLoadAttempt: no listed failure names an object chain; resolving no class owners'

            # ACT
            $actualDiagnostics = $script:Run.Diagnostics

            # ASSERT
            $actualDiagnostics | Should -Contain $expectedLine
        }
    }

    Context 'a log with no loads' {

        It 'prints an empty JSON list and says it found nothing' {

            # ARRANGE
            $expectedOutput = '[]'
            $expectedLine = 'Get-SaveLoadAttempt: grouped 0 attempts into 0 loads, 0 failed; listing them newest first'

            # ACT
            $actualRun = Invoke-SaveLoadAttempt @{ StarsectorRoot = (Get-FixtureRoot 'no-loads'); AsJson = $true }

            # ASSERT
            $actualRun.Output | Should -Be $expectedOutput
            $actualRun.Diagnostics | Should -Contain $expectedLine
        }
    }

    Context 'a missing log' {

        It 'fails naming the path' {

            # ARRANGE
            $missingLog = Join-Path $TestDrive 'starsector.log'
            $expectedMessage = "log not found: $missingLog"

            # ACT
            $actualError = { Invoke-SaveLoadAttempt @{ LogPath = $missingLog } } | Should -Throw -PassThru

            # ASSERT
            $actualError.Exception.Message | Should -Be $expectedMessage
        }

        # The outcome is checked first, so a typo fails before a log of a million lines is read.
        It 'fails on a mistyped -Outcome before looking for the log' {

            # ARRANGE
            $missingLog = Join-Path $TestDrive 'starsector.log'
            $expectedMessage = 'no load outcome failed; the outcomes are Loaded, Failed, Unfinished'

            # ACT
            $actualError = { Invoke-SaveLoadAttempt @{ LogPath = $missingLog; Outcome = 'failed' } } |
                Should -Throw -PassThru

            # ASSERT
            $actualError.Exception.Message | Should -Be $expectedMessage
        }
    }
}
