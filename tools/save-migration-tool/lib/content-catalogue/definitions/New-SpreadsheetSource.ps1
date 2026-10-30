# Describes one spreadsheet the game defines content from, and how a row of it becomes an ID.
# The game merges every source's copy of the spreadsheet by its key columns: the first row of a key wins,
# and a row whose key columns are all empty is dropped.
function New-SpreadsheetSource {

    param(
        # The spreadsheet's path under a data folder, as mod_info.json's "replace" names it.
        [string] $Path,

        [string[]] $KeyColumns = @('id'),

        # The column holding the ID, where it is not the one key column.
        # With no ID column, the key columns' values joined by IdJoiner are the ID.
        [string] $IdColumn,

        [string] $IdJoiner = '_|_',

        # A row counts only when this column holds this value.
        [string] $RowFilterColumn,
        [string] $RowFilterValue,

        # An ID opening with # is a comment to the loader.
        [switch] $SkipsHashIds,

        # A row with any key column empty defines nothing.
        [switch] $RequiresEveryKeyColumn,

        # The game turns curly quotes straight before it reads the text.
        [switch] $ConvertsCurlyQuotes
    )

    # The columns a row is read with: the key columns, then the ID and filter columns where those differ.
    $columnNames = [System.Collections.Generic.List[string]]::new($KeyColumns)

    foreach ($extraColumn in @($IdColumn, $RowFilterColumn)) {

        if ($extraColumn -and -not $columnNames.Contains($extraColumn)) {
            $columnNames.Add($extraColumn)
        }
    }

    return [pscustomobject]@{
        Path                   = $Path
        ColumnNames            = $columnNames.ToArray()
        KeyColumns             = $KeyColumns
        IdColumn               = $IdColumn
        IdJoiner               = $IdJoiner
        RowFilterColumn        = $RowFilterColumn
        RowFilterValue         = $RowFilterValue
        SkipsHashIds           = [bool] $SkipsHashIds
        RequiresEveryKeyColumn = [bool] $RequiresEveryKeyColumn
        ConvertsCurlyQuotes    = [bool] $ConvertsCurlyQuotes
    }
}
