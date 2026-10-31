# Picks the given columns out of every row of a spreadsheet ConvertFrom-StarsectorCsv read.
# Returns one array per row, in file order, holding each asked column's value in the order asked,
# or null where the header has no such column or the row ends before it. The rows come as one array, empty or not.
# Column names match case-sensitively, as the game's reader matches them.
function Select-CsvColumns {

    param(
        # The spreadsheet, as ConvertFrom-StarsectorCsv returns it.
        [object] $Table,

        [string[]] $ColumnNames
    )

    $columnIndexes = @(foreach ($columnName in $ColumnNames) {

        if ($Table.ColumnIndexByName.ContainsKey($columnName)) {
            $Table.ColumnIndexByName[$columnName]
        } else {
            -1
        }
    })

    $rows = @(foreach ($row in $Table.Rows) {

        ,@(foreach ($index in $columnIndexes) {

            if ($index -ge 0 -and $index -lt $row.Count) {
                $row[$index]
            } else {
                $null
            }
        })
    })

    return ,$rows
}
