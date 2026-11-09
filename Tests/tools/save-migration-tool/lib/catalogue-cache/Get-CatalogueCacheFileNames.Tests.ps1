BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\catalogue-cache\Get-CatalogueCacheFileNames.ps1"
}

Describe 'Get-CatalogueCacheFileNames' {

    It 'names a kind''s key and owners files in the catalogue folder' {

        # ARRANGE
        $expectedFolder = 'catalogue'
        $expectedKey = 'Hull.key.json'
        $expectedOwners = 'Hull.owners.json'

        # ACT
        $actualNames = Get-CatalogueCacheFileNames -Kind 'Hull'

        # ASSERT
        $actualNames.Folder |
            Should -Be $expectedFolder

        $actualNames.Key |
            Should -Be $expectedKey

        $actualNames.Owners |
            Should -Be $expectedOwners
    }
}
