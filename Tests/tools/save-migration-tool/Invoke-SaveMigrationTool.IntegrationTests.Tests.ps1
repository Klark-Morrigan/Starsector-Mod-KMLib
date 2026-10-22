# Integration tests for Invoke-SaveMigrationTool.ps1: the script, the scripts it runs and their lib\ functions
# run together, unmocked. Each script's own cases are pinned by its own integration suite.
# Run with: Invoke-Pester -Path .\Invoke-SaveMigrationTool.IntegrationTests.Tests.ps1
#
# Each case copies the two-failed-loads fixture's logs under TestDrive and adds the saved-mod-list fixture's
# descriptor as the failed save's, so one install holds both a failed load and its save. No test reads a real install.

BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\helpers\Invoke-ScriptCapturingOutput.ps1"

    # Tests\ mirrors the repo, so the script sits at the same relative path under the repo root.
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))

    $script:ScriptPath = Join-Path $repoRoot 'tools\save-migration-tool\Invoke-SaveMigrationTool.ps1'
    $script:FixturesRoot = Join-Path $PSScriptRoot 'fixtures'

    function script:New-DiagnosisInstall {
        # Copies a fixture's logs under TestDrive, with the saved-mod-list descriptor as save_Alpha_1's.
        param(
            [string] $Name,
            [string] $LogFixture
        )

        $root = Join-Path $TestDrive $Name
        $saveFolder = Join-Path $root 'saves\save_Alpha_1'

        Copy-Item -LiteralPath (Join-Path $script:FixturesRoot $LogFixture) -Destination $root -Recurse
        New-Item -ItemType Directory -Path $saveFolder -Force | Out-Null
        $descriptor = Join-Path $script:FixturesRoot 'saved-mod-list\saves\save_TestCaptain_1\descriptor.xml'

        Copy-Item -LiteralPath $descriptor -Destination $saveFolder

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

        It 'reports the newest failed load with its retries, then how many mods its save was last written with' {

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
                'Mods the save was last written with: 3; Get-SaveModDrift.ps1 lists them'
            )
        }

        It 'logs which save it chose, beside what the scripts it runs log' {

            $script:Run.Diagnostics |
                Should -Contain 'Invoke-SaveMigrationTool: diagnosing the save of the newest failed load, save_Alpha_1'

            $script:Run.Diagnostics | Should -Contain (
                'Get-SaveLoadAttempt: grouped 6 attempts into 2 loads, 2 failed; ' +
                'listing 1 newest first: ending Failed, the newest only')

            @($script:Run.Diagnostics -like 'Get-SaveModDrift: listing 3 enabled mods*').Count | Should -Be 1
        }

        It 'prints the report object as JSON with -AsJson' {

            $run = Invoke-SaveMigrationTool @{ StarsectorRoot = $script:Root; AsJson = $true }
            $report = $run.Output -join "`n" | ConvertFrom-Json

            $report.SaveFolder | Should -Be 'save_Alpha_1'
            $report.Load.Missing.Id | Should -Be 'BSC_Buffalo_Mk3'
            $report.Mods.Id | Should -Be @('alpha', 'beta', 'gamma')
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
                'Mods the save was last written with: 3; Get-SaveModDrift.ps1 lists them'
            )

            $run.Diagnostics | Should -Contain 'Invoke-SaveMigrationTool: diagnosing save_Alpha_1, as asked'
            $run.Diagnostics | Should -Contain (
                'Get-SaveLoadAttempt: grouped 2 attempts into 1 loads, 0 failed; ' +
                'listing 0 newest first: of save save_Alpha_1, the newest only')
        }
    }
}
