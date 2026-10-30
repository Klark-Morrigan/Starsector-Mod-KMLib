BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content-catalogue\definitions\New-ContentPart.ps1"
}

Describe 'New-ContentPart' {

    It 'holds no source of a kind it is not given' {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualPart = New-ContentPart

        # ASSERT
        $actualPart.FileSets.Count | Should -Be $expectedCount
        $actualPart.Spreadsheets.Count | Should -Be $expectedCount
        $actualPart.KeyedFiles.Count | Should -Be $expectedCount
        $actualPart.BuiltInIds.Count | Should -Be $expectedCount
    }

    It 'holds each source it is given' {

        # ARRANGE
        $expectedFileSets = @('files')
        $expectedSpreadsheets = @('rows')
        $expectedKeyedFiles = @('keyed')
        $expectedBuiltInIds = @('omen1')

        # ACT
        $actualPart = New-ContentPart -FileSets @('files') -Spreadsheets @('rows') -KeyedFiles @('keyed') `
            -BuiltInIds @('omen1')

        # ASSERT
        $actualPart.FileSets | Should -Be $expectedFileSets
        $actualPart.Spreadsheets | Should -Be $expectedSpreadsheets
        $actualPart.KeyedFiles | Should -Be $expectedKeyedFiles
        $actualPart.BuiltInIds | Should -Be $expectedBuiltInIds
    }
}
