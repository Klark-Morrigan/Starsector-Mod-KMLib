BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\New-ModDriftRow.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Compare-SaveModRoster.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Get-ModIdComparer.ps1"

    function script:New-Mod {
        # One side's entry for a mod; Side says which list it came from.
        param(
            [string] $Id,
            [string] $Side
        )

        return [pscustomobject]@{ Id = $Id; Side = $Side }
    }
}

Describe 'Compare-SaveModRoster' {

    BeforeEach {

        Mock New-ModDriftRow {

            $savedText = if ($null -ne $Saved) { "$($Saved.Id)@$($Saved.Side)" } else { '-' }
            $installedText = if ($null -ne $Installed) { "$($Installed.Id)@$($Installed.Side)" } else { '-' }

            "$savedText/$installedText"
        }
        Mock Get-ModIdComparer { [System.StringComparer]::Ordinal }
    }

    It 'pairs each saved mod with the installed one of its ID, in the save''s order, then lists the added' {

        # ARRANGE
        $savedMods = @((New-Mod 'beta' 'save'), (New-Mod 'gone' 'save'), (New-Mod 'alpha' 'save'))
        $roster = @((New-Mod 'alpha' 'install'), (New-Mod 'new' 'install'), (New-Mod 'beta' 'install'))
        $expectedRows = @(
            'beta@save/beta@install'
            'gone@save/-'
            'alpha@save/alpha@install'
            '-/new@install'
        )

        # ACT
        $actualRows = Compare-SaveModRoster -SavedMods $savedMods -Roster $roster

        # ASSERT
        $actualRows | Should -Be $expectedRows
    }

    It 'matches IDs by the mod ID comparer' {

        # ARRANGE
        $savedMods = @(New-Mod 'Alpha' 'save')
        $roster = @(New-Mod 'alpha' 'install')
        $expectedRows = @('Alpha@save/-', '-/alpha@install')

        # ACT
        $actualRows = Compare-SaveModRoster -SavedMods $savedMods -Roster $roster

        # ASSERT
        $actualRows | Should -Be $expectedRows
    }

    It 'lists every installed mod as added for a save written with none' {

        # ARRANGE
        $roster = @(New-Mod 'alpha' 'install')
        $expectedRows = @('-/alpha@install')

        # ACT
        $actualRows = Compare-SaveModRoster -SavedMods @() -Roster $roster

        # ASSERT
        $actualRows | Should -Be $expectedRows
    }

    It 'lists every saved mod as missing for an install with none' {

        # ARRANGE
        $savedMods = @(New-Mod 'alpha' 'save')
        $expectedRows = @('alpha@save/-')

        # ACT
        $actualRows = Compare-SaveModRoster -SavedMods $savedMods -Roster @()

        # ASSERT
        $actualRows | Should -Be $expectedRows
    }
}
