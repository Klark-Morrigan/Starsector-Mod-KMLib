# Integration tests for Get-SaveModDrift.ps1: the script, Get-ModRoster.ps1 and their lib\ functions run together,
# unmocked, over descriptors shaped as 0.98a writes them. Each lib function's own cases are pinned by its unit suite.
# Run with: Invoke-Pester -Path .\Get-SaveModDrift.IntegrationTests.Tests.ps1
#
# fixtures\saved-mod-list is a hand-written install root: one save's descriptor.xml, a trimmed copy of what 0.98a
# writes, and a mods\ folder. Against the save, the install keeps alpha, changes beta, lacks gamma and adds delta.
# A case that needs a damaged descriptor writes it under TestDrive. No test reads a real install or save.

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
            $script:Run.Output.Id | Should -Be @('alpha', 'beta', 'gamma', 'delta')
        }

        It 'reads a mod at the same version on both sides as unchanged, the version given as parts on both' {

            $alpha = $script:RowsById['alpha']

            $alpha.Change | Should -Be 'Unchanged'
            $alpha.SavedVersion | Should -Be '1.40.0'
            $alpha.InstalledVersion | Should -Be '1.40.0'
            $alpha.Level | Should -BeNullOrEmpty
        }

        It 'ranks a changed mod by the save''s version parts, not its version text' {

            $beta = $script:RowsById['beta']

            $beta.Change | Should -Be 'Changed'
            $beta.SavedVersion | Should -Be '1.14.1aa'
            $beta.InstalledVersion | Should -Be '1.15.0'
            $beta.Level | Should -Be 'Minor'
        }

        It 'reads a mod the install lacks as missing, with the version the save holds' {

            $gamma = $script:RowsById['gamma']

            $gamma.Change | Should -Be 'Missing'
            $gamma.SavedVersion | Should -Be '2025-03-29'
            $gamma.InstalledVersion | Should -BeNullOrEmpty
        }

        It 'reads an installed mod the save lacks as added, with its installed version and name' {

            $delta = $script:RowsById['delta']

            $delta.Change | Should -Be 'Added'
            $delta.InstalledVersion | Should -Be '0.9.1'
            $delta.Name | Should -Be 'Delta'
        }

        It 'keeps each name as the save wrote it' {
            $script:RowsById['alpha'].Name | Should -Be ' Alpha'
        }

        It 'logs what it read, the mods enabled once and dropped since, the roster, and the counts, to stderr' {

            $script:Run.Diagnostics | Should -Be @(
                ("Get-SaveModDrift: read $script:FixtureRoot\saves\$script:FixtureSave\descriptor.xml, " +
                    'saved 2026-10-16 14:05:27.228 UTC under Starsector 0.98a-RC8; ' +
                    '19 objects carry an ID for back-pointers')
                ('Get-SaveModDrift: read 3 enabled mods as the save last wrote them; ' +
                    '1 more were enabled in an earlier save and are not now: retired')
                ''
                ("Get-ModRoster: read 3 mod folders under $script:FixtureRoot\mods and 3 enabled IDs; " +
                    'listing 3 mods (0 unparsed), skipping 0 disabled and 0 superseded copies')
                ''
                ('Get-SaveModDrift: compared them with 3 installed mods: ' +
                    '1 changed, 1 missing, 1 added, 1 unchanged')
            )
        }

        It 'prints as a table of ID, both versions, change, level and name' {

            $lines = @($script:Run.Output | Out-String -Width 200) -split "`r?`n" | Where-Object { $_.Trim() }

            $lines[0] | Should -Match '^Id\s+SavedVersion\s+InstalledVersion\s+Change\s+Level\s+Name$'
            $lines[3] | Should -Match '^beta\s+1\.14\.1aa\s+1\.15\.0\s+Changed\s+Minor\s+Beta$'
        }

        It 'does not warn' {
            $script:Run.Warnings.Count | Should -Be 0
        }

        It 'prints the same rows as JSON with -AsJson' {

            $run = Invoke-SaveModDrift @{
                StarsectorRoot = $script:FixtureRoot
                SaveFolder     = $script:FixtureSave
                AsJson         = $true
            }

            $rows = $run.Output -join "`n" | ConvertFrom-Json

            $rows.Id | Should -Be @('alpha', 'beta', 'gamma', 'delta')
            $rows.Change | Should -Be @('Unchanged', 'Changed', 'Missing', 'Added')
        }
    }

    Context 'a descriptor with an empty enabledMods list' {

        It 'reads every installed mod as added' {

            $root = New-DescriptorInstall -Name 'emptyList' -SaveDataBody '<enabledMods z="2"></enabledMods>'

            New-TestMod -StarsectorRoot $root -Folder 'Alpha' -ModInfo '{ "id": "alpha", "version": "1.0" }' | Out-Null
            [System.IO.File]::WriteAllText((Join-Path $root 'mods\enabled_mods.json'), '{ "enabledMods": ["alpha"] }')

            $run = Invoke-SaveModDrift @{ StarsectorRoot = $root; SaveFolder = 'save_Test' }

            $run.Output.Id | Should -Be @('alpha')
            $run.Output.Change | Should -Be @('Added')
        }
    }

    Context 'a descriptor with no enabledMods list' {

        It 'reports no drift, and warns naming the descriptor' {

            $root = New-DescriptorInstall -Name 'noList' -SaveDataBody '<gameVersion>0.98a-RC8</gameVersion>'
            $run = Invoke-SaveModDrift @{ StarsectorRoot = $root; SaveFolder = 'save_Test' }

            $run.Output.Count | Should -Be 0
            $run.Warnings | Should -Be @(
                "the descriptor lists no enabled mods, so no drift is reported: $root\saves\save_Test\descriptor.xml")
        }
    }

    Context 'a descriptor whose back-pointer names no object' {

        It 'fails naming the back-pointer' {

            $root = New-DescriptorInstall -Name 'danglingRef' -SaveDataBody (
                '<enabledMods z="2"><EnabledModData z="3"><spec ref="9"></spec></EnabledModData></enabledMods>')

            { Invoke-SaveModDrift @{ StarsectorRoot = $root; SaveFolder = 'save_Test' } } |
                Should -Throw '<spec ref="9"> points at no object: no element has z="9"'
        }
    }

    Context 'a save folder with no descriptor' {

        It 'fails naming the path' {

            $root = Join-Path $TestDrive 'noDescriptor'

            New-Item -ItemType Directory -Path (Join-Path $root 'saves\save_Test') -Force | Out-Null

            { Invoke-SaveModDrift @{ StarsectorRoot = $root; SaveFolder = 'save_Test' } } |
                Should -Throw "descriptor not found: $root\saves\save_Test\descriptor.xml"
        }
    }
}
