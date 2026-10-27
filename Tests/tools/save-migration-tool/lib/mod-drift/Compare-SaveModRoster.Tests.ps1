BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\New-ModDriftRow.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Compare-SaveModRoster.ps1"

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
    }

    It 'pairs each saved mod with the installed one of its ID, in the save''s order, then lists the added' {

        $savedMods = @((New-Mod 'beta' 'save'), (New-Mod 'gone' 'save'), (New-Mod 'alpha' 'save'))
        $roster = @((New-Mod 'alpha' 'install'), (New-Mod 'new' 'install'), (New-Mod 'beta' 'install'))

        Compare-SaveModRoster -SavedMods $savedMods -Roster $roster | Should -Be @(
            'beta@save/beta@install'
            'gone@save/-'
            'alpha@save/alpha@install'
            '-/new@install'
        )
    }

    It 'matches IDs case-sensitively, as the game does' {

        $rows = Compare-SaveModRoster -SavedMods @(New-Mod 'Alpha' 'save') -Roster @(New-Mod 'alpha' 'install')

        $rows | Should -Be @('Alpha@save/-', '-/alpha@install')
    }

    It 'lists every installed mod as added for a save written with none' {
        Compare-SaveModRoster -SavedMods @() -Roster @(New-Mod 'alpha' 'install') | Should -Be @('-/alpha@install')
    }

    It 'lists every saved mod as missing for an install with none' {
        Compare-SaveModRoster -SavedMods @(New-Mod 'alpha' 'save') -Roster @() | Should -Be @('alpha@save/-')
    }
}
