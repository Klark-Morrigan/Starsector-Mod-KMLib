BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\New-ContentKind.ps1"
}

Describe 'New-ContentKind' {

    It 'holds the kind, its parts, and no generated IDs unless given rules' {

        $kind = New-ContentKind -Kind 'Hull' -Parts @('part one', 'part two')

        $kind.Kind | Should -Be 'Hull'
        $kind.Parts | Should -Be @('part one', 'part two')
        $kind.Generated.Count | Should -Be 0
        (New-ContentKind -Kind 'Hull' -Parts @() -Generated @('rule')).Generated | Should -Be @('rule')
    }
}
