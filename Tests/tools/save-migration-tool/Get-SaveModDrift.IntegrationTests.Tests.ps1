# Integration tests for Get-SaveModDrift.ps1: the script and its lib\ functions run together, unmocked,
# over descriptors shaped as 0.98a writes them. Each lib function's own cases are pinned by its unit suite under lib\.
# Run with: Invoke-Pester -Path .\Get-SaveModDrift.IntegrationTests.Tests.ps1
#
# fixtures\saved-mod-list is a hand-written install root holding only one save's descriptor.xml,
# a trimmed copy of what 0.98a writes. A case that needs a damaged descriptor writes it under TestDrive.
# No test reads a real save.

BeforeAll {

    . "$PSScriptRoot\helpers\Invoke-ScriptCapturingOutput.ps1"

    # Tests\ mirrors the repo, so the script sits at the same relative path under the repo root.
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))

    $script:ScriptPath = Join-Path $repoRoot 'tools\save-migration-tool\Get-SaveModDrift.ps1'
    $script:FixtureRoot = Join-Path $PSScriptRoot 'fixtures\saved-mod-list'
    $script:FixtureSave = 'save_TestCaptain_1'

    function script:Invoke-SaveModDrift {
        # Runs the script on the given arguments; the mods are the run's Output.
        param([hashtable] $Arguments)

        return Invoke-ScriptCapturingOutput -ScriptPath $script:ScriptPath -Arguments $Arguments
    }

    function script:New-DescriptorInstall {
        # Writes an install root under TestDrive holding saves\save_Test\descriptor.xml with the given body.
        param(
            [string] $Name,
            [string] $SaveDataBody
        )

        $root = Join-Path $TestDrive $Name
        $saveFolder = Join-Path $root 'saves\save_Test'

        New-Item -ItemType Directory -Path $saveFolder -Force | Out-Null

        [System.IO.File]::WriteAllText(
            (Join-Path $saveFolder 'descriptor.xml'),
            "<?xml version=`"1.0`" ?>`n<SaveGameData z=`"1`">`n$SaveDataBody`n</SaveGameData>`n")

        return $root
    }
}

Describe 'Get-SaveModDrift.IntegrationTests' {

    Context 'a descriptor whose enabled mods point back at allModsEverEnabled' {

        BeforeAll {
            $script:Run = Invoke-SaveModDrift @{ StarsectorRoot = $script:FixtureRoot; SaveFolder = $script:FixtureSave }
        }

        It 'lists each enabled mod in the descriptor''s order, following its back-pointer' {
            $script:Run.Output.Id | Should -Be @('alpha', 'beta', 'gamma')
        }

        It 'keeps each name as the save wrote it' {
            $script:Run.Output.Name | Should -Be @(' Alpha', 'Beta', 'Gamma')
        }

        It 'reads each version shape: the parts alone, a string beside its parts, a string with no parts' {
            $script:Run.Output.Version | Should -Be @('1.40.0', '1.14.1aa', '2025-03-29')
        }

        It 'logs what it read, the mods it lists, and the mods enabled once and dropped since, to stderr' {

            $script:Run.Diagnostics | Should -Be @(
                ("Get-SaveModDrift: read $script:FixtureRoot\saves\$script:FixtureSave\descriptor.xml, " +
                    'saved 2026-10-16 14:05:27.228 UTC under Starsector 0.98a-RC8; ' +
                    '19 objects carry an ID for back-pointers')
                ('Get-SaveModDrift: listing 3 enabled mods as the save last wrote them; ' +
                    '1 more were enabled in an earlier save and are not now: retired')
            )
        }

        It 'prints as a table of ID, version and name, as Get-ModRoster.ps1 prints the installed mods' {

            $lines = @($script:Run.Output | Out-String -Width 200) -split "`r?`n" | Where-Object { $_.Trim() }

            $lines[0] | Should -Match '^Id\s+Version\s+Name$'
            $lines[2] | Should -Match '^alpha\s+1\.40\.0\s+ Alpha$'
        }

        It 'does not warn' {
            $script:Run.Warnings.Count | Should -Be 0
        }

        It 'prints the same mods as JSON with -AsJson' {

            $run = Invoke-SaveModDrift @{
                StarsectorRoot = $script:FixtureRoot
                SaveFolder     = $script:FixtureSave
                AsJson         = $true
            }

            $mods = $run.Output -join "`n" | ConvertFrom-Json

            $mods.Id | Should -Be @('alpha', 'beta', 'gamma')
            $mods.Version | Should -Be @('1.40.0', '1.14.1aa', '2025-03-29')
        }
    }

    Context 'a descriptor with no enabledMods list' {

        It 'lists no mods, and warns naming the descriptor' {

            $root = New-DescriptorInstall -Name 'noList' -SaveDataBody '<gameVersion>0.98a-RC8</gameVersion>'
            $run = Invoke-SaveModDrift @{ StarsectorRoot = $root; SaveFolder = 'save_Test' }

            $run.Output.Count | Should -Be 0
            $run.Warnings | Should -Be @("the descriptor lists no enabled mods: $root\saves\save_Test\descriptor.xml")
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
