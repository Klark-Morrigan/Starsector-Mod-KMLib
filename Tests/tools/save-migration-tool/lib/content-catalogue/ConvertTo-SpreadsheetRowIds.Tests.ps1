BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content-catalogue\ConvertTo-SpreadsheetRowIds.ps1"
    . "$libRoot\content-catalogue\New-SpreadsheetSource.ps1"
}

Describe 'ConvertTo-SpreadsheetRowIds' {

    It 'keys each row by its key column, which is its ID' {

        # ARRANGE
        $rows = @(@('alpha'), @('beta'))
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv'

        $expectedKeys = @('alpha', 'beta')
        $expectedIds = @('alpha', 'beta')

        # ACT
        $actualRows = ConvertTo-SpreadsheetRowIds -Rows $rows -Spreadsheet $spreadsheet

        # ASSERT
        $actualRows.Keys | Should -Be $expectedKeys
        $actualRows.Ids | Should -Be $expectedIds
    }

    # A row whose key is empty is no row to the game; one whose ID is empty claims its key and defines nothing.
    It 'drops a row with an empty key, and keeps the key of a row with an empty ID' {

        # ARRANGE
        $rows = @(@('', 'from_empty_key'), @('row', ''), @('kept', 'id_kept'))
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -IdColumn 'effect'

        $expectedKeys = @('row', 'kept')
        $expectedIds = @('', 'id_kept')

        # ACT
        $actualRows = ConvertTo-SpreadsheetRowIds -Rows $rows -Spreadsheet $spreadsheet

        # ASSERT
        $actualRows.Keys | Should -Be $expectedKeys
        $actualRows.Ids | Should -Be $expectedIds
    }

    # The game merges by the key before it filters, so a filtered row still claims its key.
    # The filter compares case-sensitively, and a row lacking the filter column only fails it.
    It 'defines an ID only when the filter column holds the value, keeping every row''s key' {

        # ARRANGE
        $rows = @(@('star', 'STAR'), @('rock', 'PLANET'), @('ice', 'planet'), @('none', $null))
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -RowFilterColumn 'type' -RowFilterValue 'PLANET'

        $expectedKeys = @('star', 'rock', 'ice', 'none')
        $expectedIds = @('', 'rock', '', '')

        # ACT
        $actualRows = ConvertTo-SpreadsheetRowIds -Rows $rows -Spreadsheet $spreadsheet

        # ASSERT
        $actualRows.Keys | Should -Be $expectedKeys
        $actualRows.Ids | Should -Be $expectedIds
    }

    It 'joins several key columns into the key and the ID' {

        # ARRANGE
        $rows = @(@('a', 'x'), @('c', ''))
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -KeyColumns @('name', 'tags')

        $expectedKeys = @("a`0x", "c`0")
        $expectedIds = @('a_|_x', 'c_|_')

        # ACT
        $actualRows = ConvertTo-SpreadsheetRowIds -Rows $rows -Spreadsheet $spreadsheet

        # ASSERT
        $actualRows.Keys | Should -Be $expectedKeys
        $actualRows.Ids | Should -Be $expectedIds
    }

    # Both rows still claim their keys, since the game merges before it reads the ID.
    It 'defines no ID opening with # or missing a key part, when the spreadsheet says so' {

        # ARRANGE
        $rows = @(@('#b', 'x'), @('c', ''))
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -KeyColumns @('name', 'tags') -SkipsHashIds `
            -RequiresEveryKeyColumn

        $expectedKeys = @("#b`0x", "c`0")
        $expectedIds = @('', '')

        # ACT
        $actualRows = ConvertTo-SpreadsheetRowIds -Rows $rows -Spreadsheet $spreadsheet

        # ASSERT
        $actualRows.Keys | Should -Be $expectedKeys
        $actualRows.Ids | Should -Be $expectedIds
    }

    # The game fails on a row lacking a column it reads.
    It 'throws naming the column a row lacks' {

        # ARRANGE
        $rows = @(@('a', 'x'), @('b', $null))
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -IdColumn 'effect'

        $expectedMessage = 'has no effect column in every row'

        # ACT
        $actualError = { ConvertTo-SpreadsheetRowIds -Rows $rows -Spreadsheet $spreadsheet } | Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -Be $expectedMessage
    }
}
