BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\catalogue-cache\Get-FileStampHash.ps1"
    . "$libRoot\catalogue-cache\New-CatalogueCacheKey.ps1"
}

Describe 'New-CatalogueCacheKey' {

    BeforeEach {
        Mock Get-FileStampHash { "stamps of $($Paths -join '+')" }
    }

    It 'keeps the code and sources hashes, and stamps the data files' {

        # ARRANGE
        $expectedCode = 'code hash'
        $expectedMods = 'sources hash'
        $expectedDataFiles = 'stamps of C:\a.csv+C:\b.ship'

        # ACT
        $actualKey = New-CatalogueCacheKey `
            -CodeHash 'code hash' `
            -SourcesHash 'sources hash' `
            -DataFilePaths @('C:\a.csv', 'C:\b.ship')

        # ASSERT
        $actualKey.Code |
            Should -Be $expectedCode

        $actualKey.Mods |
            Should -Be $expectedMods

        $actualKey.DataFiles |
            Should -Be $expectedDataFiles
    }
}
