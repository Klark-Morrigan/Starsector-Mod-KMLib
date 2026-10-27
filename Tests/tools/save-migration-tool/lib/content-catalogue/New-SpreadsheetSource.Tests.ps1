BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\New-SpreadsheetSource.ps1"
}

Describe 'New-SpreadsheetSource' {

    It 'keys rows by id, joins several key columns with _|_, and filters, skips and converts nothing by default' {

        $source = New-SpreadsheetSource -Path 'data/hulls/wing_data.csv'

        $source.Path | Should -Be 'data/hulls/wing_data.csv'
        $source.KeyColumns | Should -Be @('id')
        $source.IdColumn | Should -BeNullOrEmpty
        $source.IdJoiner | Should -Be '_|_'
        $source.RowFilterColumn | Should -BeNullOrEmpty
        $source.SkipsHashIds | Should -BeFalse
        $source.RequiresEveryKeyColumn | Should -BeFalse
        $source.ConvertsCurlyQuotes | Should -BeFalse
    }

    It 'keeps every way a source departs from that' {

        $source = New-SpreadsheetSource -Path 'p.csv' -KeyColumns @('a', 'b') -IdColumn 'c' -IdJoiner '_' `
            -RowFilterColumn 'type' -RowFilterValue 'PLANET' -SkipsHashIds -RequiresEveryKeyColumn -ConvertsCurlyQuotes

        $source.KeyColumns | Should -Be @('a', 'b')
        $source.IdColumn | Should -Be 'c'
        $source.IdJoiner | Should -Be '_'
        $source.RowFilterColumn | Should -Be 'type'
        $source.RowFilterValue | Should -Be 'PLANET'
        $source.SkipsHashIds | Should -BeTrue
        $source.RequiresEveryKeyColumn | Should -BeTrue
        $source.ConvertsCurlyQuotes | Should -BeTrue
    }
}
