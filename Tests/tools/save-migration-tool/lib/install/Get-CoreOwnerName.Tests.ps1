BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\install\Get-CoreOwnerName.ps1"
}

Describe 'Get-CoreOwnerName' {

    It 'names the owner of the game''s own classes and content' {

        # ARRANGE
        $expectedOwner = 'vanilla'

        # ACT
        $actualOwner = Get-CoreOwnerName

        # ASSERT
        $actualOwner | Should -Be $expectedOwner
    }
}
