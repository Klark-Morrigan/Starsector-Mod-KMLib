# Integration tests for Invoke-SaveMigrationTool.ps1: the script, the scripts it runs and their lib\ functions
# run together, unmocked. Each script's own cases are pinned by its own integration suite.
# Run with: Invoke-Pester -Path .\Invoke-SaveMigrationTool.IntegrationTests.Tests.ps1
#
# Each case copies a log fixture under TestDrive and adds the saved-mod-list fixture's descriptor as the failed save's,
# and its mods\ folder as the install's, so one install holds a failed load, its save and the mods the save drifted
# from. No test reads a real install.
# A context sharing one run makes it in BeforeAll; each case's act then reads its part of that run.

BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\helpers\Invoke-ScriptCapturingOutput.ps1"

    # Tests\ mirrors the repo, so the script sits at the same relative path under the repo root.
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))

    $script:ScriptPath = Join-Path $repoRoot 'tools\save-migration-tool\Invoke-SaveMigrationTool.ps1'
    $script:FixturesRoot = Join-Path $PSScriptRoot 'fixtures'

    function script:New-DiagnosisInstall {
        # Copies a fixture's logs under TestDrive, with the saved-mod-list descriptor as save_Alpha_1's
        # and the saved-mod-list mods as the installed ones.
        param(
            [string] $Name,
            [string] $LogFixture
        )

        $root = Join-Path $TestDrive $Name
        $saveFolder = Join-Path $root 'saves\save_Alpha_1'
        $modListFixture = Join-Path $script:FixturesRoot 'saved-mod-list'
        $descriptor = Join-Path $modListFixture 'saves\save_TestCaptain_1\descriptor.xml'

        Copy-Item -LiteralPath (Join-Path $script:FixturesRoot $LogFixture) -Destination $root -Recurse
        New-Item -ItemType Directory -Path $saveFolder -Force | Out-Null
        Copy-Item -LiteralPath $descriptor -Destination $saveFolder
        Copy-Item -LiteralPath (Join-Path $modListFixture 'mods') -Destination $root -Recurse

        return $root
    }

    function script:Invoke-SaveMigrationTool {
        # Runs the script on the given arguments; the report is the run's Output.
        param([hashtable] $Arguments)

        return Invoke-ScriptCapturingOutput -ScriptPath $script:ScriptPath -Arguments $Arguments
    }
}

Describe 'Invoke-SaveMigrationTool.IntegrationTests' {

    Context 'an install whose newest load failed' {

        BeforeAll {

            $script:Root = New-DiagnosisInstall -Name 'failed' -LogFixture 'two-failed-loads'
            $script:Run = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root }
        }

        It 'reports the newest failed load with its retries, then how its save''s mods drifted' {

            # ARRANGE
            $expectedOutput = @(
                ''
                'Save: save_Alpha_1'
                'Newest load: Failed'
                '  Cause:'
                '    java.lang.RuntimeException'
                '    Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'
                '  missing Hull BSC_Buffalo_Mk3, named as BSC_Buffalo_Mk3_default_D'
                '  object chain length 6, mod classes 0'
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
                ''
                'Mod drift against the install: 1 changed, 1 missing, 1 added, 1 unchanged'
                '  Beta (beta): 1.14.1aa -> 1.15.0, minor change'
                '  Gamma (gamma): missing, saved at 2025-03-29'
                '  Delta (delta): added, installed at 0.9.1'
            )

            # ACT
            $actualOutput = $script:Run.Output

            # ASSERT
            $actualOutput | Should -Be $expectedOutput
        }

        It 'logs which save it chose and what it wrote, beside what the parts it runs log' {

            # ARRANGE
            $expectedChoiceLine = 'Invoke-SaveMigrationTool: ' +
                'diagnosing the save of the newest failed load, save_Alpha_1'
            $expectedGroupedLine = 'Get-SaveLoadAttempt: grouped 6 attempts into 2 loads, 2 failed; ' +
                'listing 1 newest first: ending Failed, the newest only'
            $expectedComparedPattern = 'Invoke-SaveMigrationTool: compared them with 3 installed mods*'
            $expectedComparedCount = 1
            $expectedLastLine = 'Invoke-SaveMigrationTool: writing the report as text, with 4 mod drift rows'

            # ACT
            $actualDiagnostics = $script:Run.Diagnostics

            # ASSERT
            $actualDiagnostics | Should -Contain $expectedChoiceLine
            $actualDiagnostics | Should -Contain $expectedGroupedLine
            @($actualDiagnostics -like $expectedComparedPattern).Count | Should -Be $expectedComparedCount
            $actualDiagnostics[-1] | Should -Be $expectedLastLine
        }

        # Both parts need the installed mods, and each read costs seconds on a large install.
        It 'reads the installed mods once' {

            # ARRANGE
            $expectedReadCount = 1

            # ACT
            $actualReadLines = @($script:Run.Diagnostics -like '*mod folders under*')

            # ASSERT
            $actualReadLines.Count | Should -Be $expectedReadCount
        }

        It 'prints the report object as JSON with -AsJson' {

            # ARRANGE
            $expectedSaveFolder = 'save_Alpha_1'
            $expectedMissingId = 'BSC_Buffalo_Mk3'
            $expectedDriftIds = @('alpha', 'beta', 'gamma', 'delta')
            $expectedDriftChanges = @('Unchanged', 'Changed', 'Missing', 'Added')

            # ACT
            $actualRun = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root; AsJson = $true }

            # ASSERT
            $actualReport = $actualRun.Output -join "`n" | ConvertFrom-Json

            $actualReport.SaveFolder | Should -Be $expectedSaveFolder
            $actualReport.Load.Missing.Id | Should -Be $expectedMissingId
            $actualReport.ModDrift.Id | Should -Be $expectedDriftIds
            $actualReport.ModDrift.Change | Should -Be $expectedDriftChanges
            $actualReport.ModDriftProblem | Should -BeNullOrEmpty
        }

        It 'diagnoses a save named with -SaveFolder whose newest load failed' {

            # ARRANGE
            $expectedHeading = @('Save: save_Alpha_1', 'Newest load: Failed')
            $expectedChoiceLine = 'Invoke-SaveMigrationTool: diagnosing save_Alpha_1, as asked'

            # ACT
            $actualRun = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root; SaveFolder = 'save_Alpha_1' }

            # ASSERT
            $actualRun.Output[1..2] | Should -Be $expectedHeading
            $actualRun.Diagnostics | Should -Contain $expectedChoiceLine
        }
    }

    # The game loads no mods from an install with no mods folder, so every mod the save names is missing.
    Context 'an install with no mods folder' {

        BeforeAll {

            $script:Root = New-DiagnosisInstall -Name 'noMods' -LogFixture 'two-failed-loads'

            Remove-Item -LiteralPath (Join-Path $script:Root 'mods') -Recurse

            $script:Run = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root; AsJson = $true }
        }

        It 'reports the load, and every saved mod as missing' {

            # ARRANGE
            $expectedSaveFolder = 'save_Alpha_1'
            $expectedMissingId = 'BSC_Buffalo_Mk3'
            $expectedDriftIds = @('alpha', 'beta', 'gamma')
            $expectedDriftChanges = @('Missing', 'Missing', 'Missing')

            # ACT
            $actualReport = $script:Run.Output -join "`n" | ConvertFrom-Json

            # ASSERT
            $actualReport.SaveFolder | Should -Be $expectedSaveFolder
            $actualReport.Load.Missing.Id | Should -Be $expectedMissingId
            $actualReport.ModDrift.Id | Should -Be $expectedDriftIds
            $actualReport.ModDrift.Change | Should -Be $expectedDriftChanges
        }

        It 'warns once that it lists no mods' {

            # ARRANGE
            $expectedWarning = "mods directory not found, listing no mods: $script:Root\mods"
            $expectedWarningCount = 1

            # ACT
            $actualWarnings = @($script:Run.Warnings -eq $expectedWarning)

            # ASSERT
            $actualWarnings.Count | Should -Be $expectedWarningCount
        }
    }

    Context 'a failed load whose save is gone' {

        # The load's diagnosis is worth reporting though the save cannot be compared.
        It 'reports the load, and says the mods were not compared and why' {

            # ARRANGE
            $root = New-DiagnosisInstall -Name 'saveGone' -LogFixture 'two-failed-loads'
            $descriptorPath = Join-Path $root 'saves\save_Alpha_1\descriptor.xml'

            Remove-Item -LiteralPath $descriptorPath

            $expectedLoadLine = 'Newest load: Failed'
            $expectedDriftLine = "Mod drift against the install: not compared, descriptor not found: $descriptorPath"
            $expectedProblemLine = "Invoke-SaveMigrationTool: comparing no mods: descriptor not found: $descriptorPath"

            # ACT
            $actualRun = Invoke-SaveMigrationTool @{ StarsectorRoot = $root }

            # ASSERT
            $actualRun.Output[2] | Should -Be $expectedLoadLine
            $actualRun.Output[-1] | Should -Be $expectedDriftLine
            $actualRun.Diagnostics | Should -Contain $expectedProblemLine
        }
    }

    Context 'a failed load whose descriptor lists no enabled mods' {

        BeforeAll {

            $script:Root = New-DiagnosisInstall -Name 'noList' -LogFixture 'two-failed-loads'
            $script:DescriptorPath = Join-Path $script:Root 'saves\save_Alpha_1\descriptor.xml'

            $descriptorText = "<?xml version=`"1.0`" ?>`n<SaveGameData z=`"1`">`n" +
                "<gameVersion>0.98a-RC8</gameVersion>`n</SaveGameData>`n"

            [System.IO.File]::WriteAllText($script:DescriptorPath, $descriptorText)
        }

        # Counting nothing as "0 changed" would read as a save that matches the install.
        It 'says in text that the mods were not compared and why' {

            # ARRANGE
            $expectedDriftLine = 'Mod drift against the install: not compared, ' +
                "the descriptor lists no enabled mods: $script:DescriptorPath"

            # ACT
            $actualRun = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root }

            # ASSERT
            $actualRun.Output[-1] | Should -Be $expectedDriftLine
        }

        It 'says in JSON that the mods were not compared and why' {

            # ARRANGE
            $expectedProblem = "the descriptor lists no enabled mods: $script:DescriptorPath"

            # ACT
            $actualRun = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root; AsJson = $true }

            # ASSERT
            $actualReport = $actualRun.Output -join "`n" | ConvertFrom-Json

            $actualReport.ModDrift | Should -BeNullOrEmpty
            $actualReport.ModDriftProblem | Should -Be $expectedProblem
        }
    }

    Context 'an install whose logs hold no failed load' {

        BeforeAll {
            $script:Root = New-DiagnosisInstall -Name 'loaded' -LogFixture 'recovered-from-bak'
        }

        It 'says no save is diagnosed' {

            # ARRANGE
            $expectedOutput = @(
                ''
                'No load failed in the logs, so no save is diagnosed. Name one with -SaveFolder.'
            )
            $expectedLine = 'Invoke-SaveMigrationTool: no load failed, so no save is diagnosed'

            # ACT
            $actualRun = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root }

            # ASSERT
            $actualRun.Output | Should -Be $expectedOutput
            $actualRun.Diagnostics | Should -Contain $expectedLine
        }

        It 'diagnoses a save named with -SaveFolder, though the logs hold no load of it' {

            # ARRANGE
            $expectedOutput = @(
                ''
                'Save: save_Alpha_1'
                'Newest load: none in the logs'
                ''
                'Mod drift against the install: 1 changed, 1 missing, 1 added, 1 unchanged'
                '  Beta (beta): 1.14.1aa -> 1.15.0, minor change'
                '  Gamma (gamma): missing, saved at 2025-03-29'
                '  Delta (delta): added, installed at 0.9.1'
            )
            $expectedChoiceLine = 'Invoke-SaveMigrationTool: diagnosing save_Alpha_1, as asked'
            $expectedGroupedLine = 'Get-SaveLoadAttempt: grouped 2 attempts into 1 loads, 0 failed; ' +
                'listing 0 newest first: of save save_Alpha_1, the newest only'

            # ACT
            $actualRun = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root; SaveFolder = 'save_Alpha_1' }

            # ASSERT
            $actualRun.Output | Should -Be $expectedOutput
            $actualRun.Diagnostics | Should -Contain $expectedChoiceLine
            $actualRun.Diagnostics | Should -Contain $expectedGroupedLine
        }
    }
}
