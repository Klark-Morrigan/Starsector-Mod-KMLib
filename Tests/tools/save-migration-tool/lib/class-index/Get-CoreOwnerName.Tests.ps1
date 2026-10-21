BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index\Get-CoreOwnerName.ps1"
}

Describe 'Get-CoreOwnerName' {

    It 'names the owner of the game''s own classes' {
        Get-CoreOwnerName | Should -Be 'vanilla'
    }
}
