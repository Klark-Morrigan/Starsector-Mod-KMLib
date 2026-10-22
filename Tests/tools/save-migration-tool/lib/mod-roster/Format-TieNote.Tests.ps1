BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Format-ModVersion.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Format-TieNote.ps1"

    function script:New-Copy {
        # One installed copy of a mod, as far as the note reads it.
        param(
            [string] $SortKey,
            [string] $Version
        )

        return [pscustomobject]@{ SortKey = $SortKey; Version = $Version }
    }
}

Describe 'Format-TieNote' {

    BeforeEach {
        Mock Format-ModVersion { "$Version" }
    }

    It 'says nothing when the version numbers differ' {
        Format-TieNote -Winner (New-Copy '2' '2.0') -Loser (New-Copy '1' '1.0') | Should -Be ''
    }

    It 'says the version as written decided when only the numbers tie' {

        Format-TieNote -Winner (New-Copy '1.5' '1.5b') -Loser (New-Copy '1.5' '1.5a') |
            Should -Be ', the same version number, so the higher version as written'
    }

    It 'says the folder name decided at the very same version' {

        Format-TieNote -Winner (New-Copy '1' '1.0') -Loser (New-Copy '1' '1.0') |
            Should -Be ', the same version, so first by folder name'
    }
}
