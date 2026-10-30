BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content-catalogue\definitions\New-SpreadsheetSource.ps1"
}

Describe 'New-SpreadsheetSource' {

    It 'keys rows by id, joins several key columns with _|_, and filters, skips and converts nothing by default' {

        # ARRANGE
        $expectedPath = 'data/hulls/wing_data.csv'
        $expectedKeyColumns = @('id')
        $expectedColumnNames = @('id')
        $expectedIdJoiner = '_|_'

        # ACT
        $actualSource = New-SpreadsheetSource -Path 'data/hulls/wing_data.csv'

        # ASSERT
        $actualSource.Path | Should -Be $expectedPath
        $actualSource.KeyColumns | Should -Be $expectedKeyColumns
        $actualSource.ColumnNames | Should -Be $expectedColumnNames
        $actualSource.IdColumn | Should -BeNullOrEmpty
        $actualSource.IdJoiner | Should -Be $expectedIdJoiner
        $actualSource.RowFilterColumn | Should -BeNullOrEmpty
        $actualSource.SkipsHashIds | Should -BeFalse
        $actualSource.RequiresEveryKeyColumn | Should -BeFalse
        $actualSource.ConvertsCurlyQuotes | Should -BeFalse
    }

    It 'keeps every way a source departs from that' {

        # ARRANGE
        $expectedKeyColumns = @('a', 'b')
        $expectedIdColumn = 'c'
        $expectedIdJoiner = '_'
        $expectedRowFilterColumn = 'type'
        $expectedRowFilterValue = 'PLANET'

        # ACT
        $actualSource = New-SpreadsheetSource -Path 'p.csv' -KeyColumns @('a', 'b') -IdColumn 'c' -IdJoiner '_' `
            -RowFilterColumn 'type' -RowFilterValue 'PLANET' -SkipsHashIds -RequiresEveryKeyColumn -ConvertsCurlyQuotes

        # ASSERT
        $actualSource.KeyColumns | Should -Be $expectedKeyColumns
        $actualSource.IdColumn | Should -Be $expectedIdColumn
        $actualSource.IdJoiner | Should -Be $expectedIdJoiner
        $actualSource.RowFilterColumn | Should -Be $expectedRowFilterColumn
        $actualSource.RowFilterValue | Should -Be $expectedRowFilterValue
        $actualSource.SkipsHashIds | Should -BeTrue
        $actualSource.RequiresEveryKeyColumn | Should -BeTrue
        $actualSource.ConvertsCurlyQuotes | Should -BeTrue
    }

    # Each row holds its columns in this order, so the key columns come first.
    It 'reads the key columns, then the ID and filter columns' {

        # ARRANGE
        $expectedColumnNames = @('name', 'tags', 'id', 'type')

        # ACT
        $actualSource = New-SpreadsheetSource -Path 'p.csv' -KeyColumns @('name', 'tags') -IdColumn 'id' `
            -RowFilterColumn 'type'

        # ASSERT
        $actualSource.ColumnNames | Should -Be $expectedColumnNames
    }

    # The reader is asked for each column once, though the ID or filter column is also a key column.
    It 'reads a column named twice once, comparing names case-sensitively' {

        # ARRANGE
        $expectedColumnNames = @('id', 'type', 'ID')

        # ACT
        $actualSource = New-SpreadsheetSource -Path 'p.csv' -KeyColumns @('id', 'type') -IdColumn 'ID' `
            -RowFilterColumn 'type'

        # ASSERT
        $actualSource.ColumnNames | Should -Be $expectedColumnNames
    }
}
