# Integration tests for Get-SaveModDrift.ps1: the script, Get-ModRoster.ps1 and their lib\ functions run together,
# unmocked, over descriptors shaped as 0.98a writes them. Each lib function's own cases are pinned by its unit suite.
# Run with: Invoke-Pester -Path .\Get-SaveModDrift.IntegrationTests.Tests.ps1
#
# fixtures\saved-mod-list is a hand-written install root: one save's descriptor.xml, a trimmed copy of what 0.98a
# writes, and a mods\ folder. Against the save, the install keeps alpha, changes beta, lacks gamma and adds delta.
# A case that needs a damaged descriptor writes it under TestDrive. No test reads a real install or save.
# A context sharing one run makes it in BeforeAll; each case's act then reads its part of that run.

BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\helpers\Invoke-ScriptCapturingOutput.ps1"
    . "$PSScriptRoot\helpers\New-TestMod.ps1"

    # Tests\ mirrors the repo, so the script sits at the same relative path under the repo root.
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))

    $script:ScriptPath = Join-Path $repoRoot 'tools\save-migration-tool\Get-SaveModDrift.ps1'
    $script:FixtureRoot = Join-Path $PSScriptRoot 'fixtures\saved-mod-list'
    $script:FixtureSave = 'save_TestCaptain_1'

    function script:Invoke-SaveModDrift {
        # Runs the script on the given arguments; the drift rows are the run's Output.
        param([hashtable] $Arguments)

        return Invoke-ScriptCapturingOutput -ScriptPath $script:ScriptPath -Arguments $Arguments
    }

    function script:New-DescriptorInstall {
        # Writes an install root under TestDrive holding saves\save_Test\descriptor.xml with the given body,
        # and a mods\ folder with no mod in it.
        param(
            [string] $Name,
            [string] $SaveDataBody
        )

        $root = Join-Path $TestDrive $Name
        $saveFolder = Join-Path $root 'saves\save_Test'

        New-Item -ItemType Directory -Path $saveFolder -Force | Out-Null
        New-Item -ItemType Directory -Path (Join-Path $root 'mods') -Force | Out-Null

        [System.IO.File]::WriteAllText(
            (Join-Path $saveFolder 'descriptor.xml'),
            "<?xml version=`"1.0`" ?>`n<SaveGameData z=`"1`">`n$SaveDataBody`n</SaveGameData>`n")

        return $root
    }
}

Describe 'Get-SaveModDrift.IntegrationTests' {

    Context 'a save whose mods the install keeps, changes, lacks and adds to' {

        BeforeAll {

            $script:Run = Invoke-SaveModDrift @{
                StarsectorRoot = $script:FixtureRoot
                SaveFolder     = $script:FixtureSave
            }

            $script:RowsById = @{}

            foreach ($row in $script:Run.Output) {
                $script:RowsById[$row.Id] = $row
            }
        }

        It 'lists the save''s mods in the descriptor''s order, following each back-pointer, then the added' {

            # ARRANGE
            $expectedIds = @('alpha', 'beta', 'gamma', 'delta')

            # ACT
            $actualIds = $script:Run.Output.Id

            # ASSERT
            $actualIds | Should -Be $expectedIds
        }

        It 'reads a mod at the same version on both sides as unchanged, the version given as parts on both' {

            # ARRANGE
            $expectedChange = 'Unchanged'
            $expectedSavedVersion = '1.40.0'
            $expectedInstalledVersion = '1.40.0'

            # ACT
            $actualAlpha = $script:RowsById['alpha']

            # ASSERT
            $actualAlpha.Change | Should -Be $expectedChange
            $actualAlpha.SavedVersion | Should -Be $expectedSavedVersion
            $actualAlpha.InstalledVersion | Should -Be $expectedInstalledVersion
            $actualAlpha.Level | Should -BeNullOrEmpty
        }

        It 'ranks a changed mod by the save''s version parts, not its version text' {

            # ARRANGE
            $expectedChange = 'Changed'
            $expectedSavedVersion = '1.14.1aa'
            $expectedInstalledVersion = '1.15.0'
            $expectedLevel = 'Minor'

            # ACT
            $actualBeta = $script:RowsById['beta']

            # ASSERT
            $actualBeta.Change | Should -Be $expectedChange
            $actualBeta.SavedVersion | Should -Be $expectedSavedVersion
            $actualBeta.InstalledVersion | Should -Be $expectedInstalledVersion
            $actualBeta.Level | Should -Be $expectedLevel
        }

        It 'reads a mod the install lacks as missing, with the version the save holds' {

            # ARRANGE
            $expectedChange = 'Missing'
            $expectedSavedVersion = '2025-03-29'

            # ACT
            $actualGamma = $script:RowsById['gamma']

            # ASSERT
            $actualGamma.Change | Should -Be $expectedChange
            $actualGamma.SavedVersion | Should -Be $expectedSavedVersion
            $actualGamma.InstalledVersion | Should -BeNullOrEmpty
        }

        It 'reads an installed mod the save lacks as added, with its installed version and name' {

            # ARRANGE
            $expectedChange = 'Added'
            $expectedInstalledVersion = '0.9.1'
            $expectedName = 'Delta'

            # ACT
            $actualDelta = $script:RowsById['delta']

            # ASSERT
            $actualDelta.Change | Should -Be $expectedChange
            $actualDelta.InstalledVersion | Should -Be $expectedInstalledVersion
            $actualDelta.Name | Should -Be $expectedName
        }

        # The installed mod_info.json names alpha "Alpha"; the save's leading space shows which side won.
        It 'keeps each name as the save wrote it' {

            # ARRANGE
            $expectedName = ' Alpha'

            # ACT
            $actualName = $script:RowsById['alpha'].Name

            # ASSERT
            $actualName | Should -Be $expectedName
        }

        It 'logs what it read, the mods enabled once and dropped since, the roster, and the counts, to stderr' {

            # ARRANGE
            $expectedDiagnostics = @(
                ("Get-SaveModDrift: read 3 mod folders under $script:FixtureRoot\mods and 3 enabled IDs; " +
                    'listing 3 mods (0 unparsed), skipping 0 disabled and 0 superseded copies')
                ("Get-SaveModDrift: read $script:FixtureRoot\saves\$script:FixtureSave\descriptor.xml, " +
                    'saved 2026-10-16 14:05:27.228 UTC under Starsector 0.98a-RC8; ' +
                    '19 objects carry an ID for back-pointers')
                ('Get-SaveModDrift: read 3 enabled mods as the save last wrote them; ' +
                    '1 more were enabled in an earlier save and are not now: retired')
                ('Get-SaveModDrift: compared them with 3 installed mods: ' +
                    '1 changed, 1 missing, 1 added, 1 unchanged')
            )

            # ACT
            $actualDiagnostics = $script:Run.Diagnostics

            # ASSERT
            $actualDiagnostics | Should -Be $expectedDiagnostics
        }

        It 'prints as a table of ID, both versions, change, level and name' {

            # ARRANGE
            $expectedHeaderPattern = '^Id\s+SavedVersion\s+InstalledVersion\s+Change\s+Level\s+Name$'
            $expectedBetaRowPattern = '^beta\s+1\.14\.1aa\s+1\.15\.0\s+Changed\s+Minor\s+Beta$'

            # ACT
            $actualLines = @($script:Run.Output | Out-String -Width 200) -split "`r?`n" | Where-Object { $_.Trim() }

            # ASSERT
            $actualLines[0] | Should -Match $expectedHeaderPattern
            $actualLines[3] | Should -Match $expectedBetaRowPattern
        }

        It 'does not warn' {

            # ARRANGE
            $expectedWarningCount = 0

            # ACT
            $actualWarningCount = $script:Run.Warnings.Count

            # ASSERT
            $actualWarningCount | Should -Be $expectedWarningCount
        }

        It 'prints the same rows as JSON with -AsJson' {

            # ARRANGE
            $arguments = @{
                StarsectorRoot = $script:FixtureRoot
                SaveFolder     = $script:FixtureSave
                AsJson         = $true
            }
            $expectedIds = @('alpha', 'beta', 'gamma', 'delta')
            $expectedChanges = @('Unchanged', 'Changed', 'Missing', 'Added')

            # ACT
            $actualRun = Invoke-SaveModDrift $arguments

            # ASSERT
            $actualRows = $actualRun.Output -join "`n" | ConvertFrom-Json

            $actualRows.Id | Should -Be $expectedIds
            $actualRows.Change | Should -Be $expectedChanges
        }
    }

    Context 'a descriptor with an empty enabledMods list' {

        It 'reads every installed mod as added' {

            # ARRANGE
            $root = New-DescriptorInstall -Name 'emptyList' -SaveDataBody '<enabledMods z="2"></enabledMods>'

            New-TestMod -StarsectorRoot $root -Folder 'Alpha' `
                -ModInfo '{ "id": "alpha", "name": "Alpha", "description": "", "version": "1.0" }' | Out-Null
            [System.IO.File]::WriteAllText((Join-Path $root 'mods\enabled_mods.json'), '{ "enabledMods": ["alpha"] }')

            $expectedIds = @('alpha')
            $expectedChanges = @('Added')

            # ACT
            $actualRun = Invoke-SaveModDrift @{ StarsectorRoot = $root; SaveFolder = 'save_Test' }

            # ASSERT
            $actualRun.Output.Id | Should -Be $expectedIds
            $actualRun.Output.Change | Should -Be $expectedChanges
        }
    }

    Context 'a descriptor with no enabledMods list' {

        It 'reports no drift, and warns naming the descriptor' {

            # ARRANGE
            $root = New-DescriptorInstall -Name 'noList' -SaveDataBody '<gameVersion>0.98a-RC8</gameVersion>'
            $expectedCount = 0
            $expectedWarning =
                "the descriptor lists no enabled mods: $root\saves\save_Test\descriptor.xml; no drift is reported"

            # ACT
            $actualRun = Invoke-SaveModDrift @{ StarsectorRoot = $root; SaveFolder = 'save_Test' }

            # ASSERT
            $actualRun.Output.Count | Should -Be $expectedCount
            $actualRun.Warnings | Should -Contain $expectedWarning
        }
    }

    Context 'a descriptor whose back-pointer names no object' {

        It 'fails naming the back-pointer' {

            # ARRANGE
            $root = New-DescriptorInstall -Name 'danglingRef' -SaveDataBody (
                '<enabledMods z="2"><EnabledModData z="3"><spec ref="9"></spec></EnabledModData></enabledMods>')
            $expectedMessage = '<spec ref="9"> points at no object: no element has z="9"'

            # ACT
            $actualError = { Invoke-SaveModDrift @{ StarsectorRoot = $root; SaveFolder = 'save_Test' } } |
                Should -Throw -PassThru

            # ASSERT
            $actualError.Exception.Message | Should -Be $expectedMessage
        }
    }

    Context 'a save folder with no descriptor' {

        It 'reports no drift, and warns naming the path' {

            # ARRANGE
            $root = Join-Path $TestDrive 'noDescriptor'

            New-Item -ItemType Directory -Path (Join-Path $root 'saves\save_Test') -Force | Out-Null
            New-Item -ItemType Directory -Path (Join-Path $root 'mods') -Force | Out-Null

            $expectedCount = 0
            $expectedWarning = "descriptor not found: $root\saves\save_Test\descriptor.xml; no drift is reported"

            # ACT
            $actualRun = Invoke-SaveModDrift @{ StarsectorRoot = $root; SaveFolder = 'save_Test' }

            # ASSERT
            $actualRun.Output.Count | Should -Be $expectedCount
            $actualRun.Warnings | Should -Contain $expectedWarning
        }
    }
}
