# Integration tests for Invoke-SaveMigrationTool.ps1: the script, the scripts it runs and their lib\ functions
# run together, unmocked. Each script's own cases are pinned by its own integration suite.
# Run with: Invoke-Pester -Path .\Invoke-SaveMigrationTool.IntegrationTests.Tests.ps1
#
# Each case copies a log fixture under TestDrive and adds the saved-mod-list fixture's descriptor as the failed save's,
# and its mods\ folder as the install's, so one install holds a failed load, its save and the mods the save drifted
# from. No test reads a real install.

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

            $script:Run.Output | Should -Be @(
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
        }

        It 'logs which save it chose and what it wrote, beside what the parts it runs log' {

            $script:Run.Diagnostics |
                Should -Contain 'Invoke-SaveMigrationTool: diagnosing the save of the newest failed load, save_Alpha_1'

            $script:Run.Diagnostics | Should -Contain (
                'Get-SaveLoadAttempt: grouped 6 attempts into 2 loads, 2 failed; ' +
                'listing 1 newest first: ending Failed, the newest only')

            @($script:Run.Diagnostics -like 'Invoke-SaveMigrationTool: compared them with 3 installed mods*').Count |
                Should -Be 1

            $script:Run.Diagnostics[-1] |
                Should -Be 'Invoke-SaveMigrationTool: writing the report as text, with 4 mod drift rows'
        }

        # Both parts need the installed mods, and each read costs seconds on a large install.
        It 'reads the installed mods once' {
            @($script:Run.Diagnostics -like '*mod folders under*').Count | Should -Be 1
        }

        It 'prints the report object as JSON with -AsJson' {

            $run = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root; AsJson = $true }
            $report = $run.Output -join "`n" | ConvertFrom-Json

            $report.SaveFolder | Should -Be 'save_Alpha_1'
            $report.Load.Missing.Id | Should -Be 'BSC_Buffalo_Mk3'
            $report.ModDrift.Id | Should -Be @('alpha', 'beta', 'gamma', 'delta')
            $report.ModDrift.Change | Should -Be @('Unchanged', 'Changed', 'Missing', 'Added')
            $report.ModDriftProblem | Should -BeNullOrEmpty
        }

        It 'diagnoses a save named with -SaveFolder whose newest load failed' {

            $run = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root; SaveFolder = 'save_Alpha_1' }

            $run.Output[1..2] | Should -Be @('Save: save_Alpha_1', 'Newest load: Failed')
            $run.Diagnostics | Should -Contain 'Invoke-SaveMigrationTool: diagnosing save_Alpha_1, as asked'
        }
    }

    Context 'a failed load whose save is gone' {

        # The load's diagnosis is worth reporting though the save cannot be compared.
        It 'reports the load, and says the mods were not compared and why' {

            $root = New-DiagnosisInstall -Name 'saveGone' -LogFixture 'two-failed-loads'
            $descriptorPath = Join-Path $root 'saves\save_Alpha_1\descriptor.xml'

            Remove-Item -LiteralPath $descriptorPath

            $run = Invoke-SaveMigrationTool @{ StarsectorRoot = $root }

            $run.Output[2] | Should -Be 'Newest load: Failed'
            $run.Output[-1] |
                Should -Be "Mod drift against the install: not compared, descriptor not found: $descriptorPath"
            $run.Diagnostics |
                Should -Contain "Invoke-SaveMigrationTool: comparing no mods: descriptor not found: $descriptorPath"
        }
    }

    Context 'a failed load whose descriptor lists no enabled mods' {

        # Counting nothing as "0 changed" would read as a save that matches the install.
        It 'says the mods were not compared and why, in text and in JSON' {

            $root = New-DiagnosisInstall -Name 'noList' -LogFixture 'two-failed-loads'
            $descriptorPath = Join-Path $root 'saves\save_Alpha_1\descriptor.xml'

            $descriptorText = "<?xml version=`"1.0`" ?>`n<SaveGameData z=`"1`">`n" +
                "<gameVersion>0.98a-RC8</gameVersion>`n</SaveGameData>`n"

            [System.IO.File]::WriteAllText($descriptorPath, $descriptorText)

            $run = Invoke-SaveMigrationTool @{ StarsectorRoot = $root }
            $jsonRun = Invoke-SaveMigrationTool @{ StarsectorRoot = $root; AsJson = $true }
            $report = $jsonRun.Output -join "`n" | ConvertFrom-Json
            $problem = "the descriptor lists no enabled mods: $descriptorPath"

            $run.Output[-1] | Should -Be "Mod drift against the install: not compared, $problem"
            $report.ModDrift | Should -BeNullOrEmpty
            $report.ModDriftProblem | Should -Be $problem
        }
    }

    Context 'an install whose logs hold no failed load' {

        BeforeAll {
            $script:Root = New-DiagnosisInstall -Name 'loaded' -LogFixture 'recovered-from-bak'
        }

        It 'says no save is diagnosed' {

            $run = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root }

            $run.Output | Should -Be @(
                ''
                'No load failed in the logs, so no save is diagnosed. Name one with -SaveFolder.'
            )
            $run.Diagnostics | Should -Contain 'Invoke-SaveMigrationTool: no load failed, so no save is diagnosed'
        }

        It 'diagnoses a save named with -SaveFolder, though the logs hold no load of it' {

            $run = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root; SaveFolder = 'save_Alpha_1' }

            $run.Output | Should -Be @(
                ''
                'Save: save_Alpha_1'
                'Newest load: none in the logs'
                ''
                'Mod drift against the install: 1 changed, 1 missing, 1 added, 1 unchanged'
                '  Beta (beta): 1.14.1aa -> 1.15.0, minor change'
                '  Gamma (gamma): missing, saved at 2025-03-29'
                '  Delta (delta): added, installed at 0.9.1'
            )

            $run.Diagnostics | Should -Contain 'Invoke-SaveMigrationTool: diagnosing save_Alpha_1, as asked'
            $run.Diagnostics | Should -Contain (
                'Get-SaveLoadAttempt: grouped 2 attempts into 1 loads, 0 failed; ' +
                'listing 0 newest first: of save save_Alpha_1, the newest only')
        }
    }
}
