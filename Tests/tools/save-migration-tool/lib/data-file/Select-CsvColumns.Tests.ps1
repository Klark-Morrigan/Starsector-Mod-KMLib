BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Select-CsvColumns.ps1"

    function script:New-Table {
        # A spreadsheet as ConvertFrom-StarsectorCsv returns it: the header's names in order, and each row's values.
        param(
            [string[]] $ColumnNames,
            [object[]] $Rows
        )

        $columnIndexByName = [System.Collections.Generic.Dictionary[string, int]]::new()

        for ($index = 0; $index -lt $ColumnNames.Count; $index++) {
            $columnIndexByName[$ColumnNames[$index]] = $index
        }

        return [pscustomobject]@{ ColumnIndexByName = $columnIndexByName; Rows = $Rows }
    }

    function script:Select-CsvLines {
        # Picks the columns and writes each row as one line, values joined by |, a missing value as <null>.
        param(
            [object] $Table,
            [string[]] $ColumnNames
        )

        $rows = Select-CsvColumns -Table $Table -ColumnNames $ColumnNames

        return @(foreach ($row in $rows) {
            ($row | ForEach-Object { if ($null -eq $_) { '<null>' } else { $_ } }) -join '|'
        })
    }
}

Describe 'Select-CsvColumns' {

    It 'returns the asked columns of each row, in the order asked, rows in file order' {

        # ARRANGE
        $table = New-Table -ColumnNames 'id', 'name', 'tier' -Rows @(
            , @('alpha', 'Alpha', '1')
            , @('beta', 'Beta', '2')
        )

        $expectedLines = @('1|alpha', '2|beta')

        # ACT
        $actualLines = Select-CsvLines -Table $table -ColumnNames 'tier', 'id'

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'gives null for a column the header lacks, matching names case-sensitively' {

        # ARRANGE
        $table = New-Table -ColumnNames 'ID', 'name' -Rows @(, @('alpha', 'Alpha'))

        $expectedLines = @('<null>|Alpha')

        # ACT
        $actualLines = Select-CsvLines -Table $table -ColumnNames 'id', 'name'

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'gives null for a column a short row ends before' {

        # ARRANGE
        $table = New-Table -ColumnNames 'name', 'id' -Rows @(
            , @('alpha', 'a')
            , @('short')
        )

        $expectedLines = @('a', '<null>')

        # ACT
        $actualLines = Select-CsvLines -Table $table -ColumnNames 'id'

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'returns <Description> as one array of rows, each an array' -ForEach @(
        @{ Description = 'no rows'; Rows = @(); ExpectedCount = 0 }
        @{ Description = 'one row'; Rows = @(, @('alpha')); ExpectedCount = 1 }
    ) {

        # ARRANGE
        $table = New-Table -ColumnNames 'id' -Rows $Rows

        $expectedCount = $ExpectedCount

        # ACT
        $actualRows = Select-CsvColumns -Table $table -ColumnNames 'id'

        # ASSERT
        Should -ActualValue $actualRows -BeOfType [object[]]
        $actualRows.Count | Should -Be $expectedCount

        foreach ($actualRow in $actualRows) {
            Should -ActualValue $actualRow -BeOfType [object[]]
        }
    }
}
