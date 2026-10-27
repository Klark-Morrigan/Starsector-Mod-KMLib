# Counts a save's mod drift rows by change, as one phrase: changed, missing, added, then unchanged.
function Format-ModDriftCounts {

    param([object[]] $Rows)

    $changes = Get-ModDriftChangeNames
    $countedChanges = @($changes.Changed, $changes.Missing, $changes.Added, $changes.Unchanged)

    # A foreach over no rows runs no iteration, where a pipeline would pass one null through.
    $counts = foreach ($change in $countedChanges) {

        $count = @(foreach ($row in $Rows) { if ($row.Change -eq $change) { $row } }).Count

        "$(Format-ConsoleNumber $count) $($change.ToLowerInvariant())"
    }

    return $counts -join ', '
}
