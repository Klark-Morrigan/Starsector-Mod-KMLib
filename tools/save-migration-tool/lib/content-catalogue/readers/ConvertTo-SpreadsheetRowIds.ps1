# Reads one copy's rows as the game sees them before it merges the copies:
# each row's merge key, and the ID the row defines, or an empty one when it defines none, at the same index.
# The game drops a row whose key columns are all empty, so that row has no key here.
# It merges by the key before it applies the row filter and reads the ID,
# so a row defining nothing still claims its key.
# A row lacking a column the game reads fails the copy, as it fails the game.
function ConvertTo-SpreadsheetRowIds {

    param(
        # The copy's rows, each an array holding the spreadsheet's ColumnNames in order.
        [object[]] $Rows,

        # The spreadsheet, as New-SpreadsheetSource describes it.
        [object] $Spreadsheet
    )

    $hashMark = '#'
    $keyPartSeparator = "`0"

    $columnNames = $Spreadsheet.ColumnNames
    $keyCount = $Spreadsheet.KeyColumns.Count
    $idIndex = [array]::IndexOf($columnNames, $Spreadsheet.IdColumn)
    $filterIndex = [array]::IndexOf($columnNames, $Spreadsheet.RowFilterColumn)

    $keys = [System.Collections.Generic.List[string]]::new()
    $ids = [System.Collections.Generic.List[string]]::new()

    foreach ($row in $Rows) {

        $keyParts = $row[0..($keyCount - 1)]

        for ($column = 0; $column -lt $columnNames.Count; $column++) {

            if ($null -eq $row[$column] -and $column -ne $filterIndex) {
                throw "has no $($columnNames[$column]) column in every row"
            }
        }

        if (-not ($keyParts -join '')) {
            continue
        }

        $isFiltered = $filterIndex -ge 0 -and $row[$filterIndex] -cne $Spreadsheet.RowFilterValue
        $isPartialKey = $Spreadsheet.RequiresEveryKeyColumn -and
            @($keyParts | Where-Object { -not $_ }).Count -gt 0

        $id = if ($idIndex -ge 0) {
            $row[$idIndex]
        } else {
            $keyParts -join $Spreadsheet.IdJoiner
        }

        $isDefining = -not $isFiltered -and -not $isPartialKey -and $id -and
            -not ($Spreadsheet.SkipsHashIds -and $id.StartsWith($hashMark))

        $definedId = if ($isDefining) {
            $id
        } else {
            ''
        }

        $keys.Add($keyParts -join $keyPartSeparator)
        $ids.Add($definedId)
    }

    return [pscustomobject]@{
        Keys = $keys
        Ids  = $ids
    }
}
