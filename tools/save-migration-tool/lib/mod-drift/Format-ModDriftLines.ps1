# Writes a save's mod drift for a person: a line counting each kind of change,
# then one line per mod that differs, changed first, then missing, then added.
# Unchanged mods are only counted, since they give a person nothing to act on.
function Format-ModDriftLines {

    param([object[]] $Rows)

    $changes = Get-ModDriftChangeNames

    "Mod drift against the install: $(Format-ModDriftCounts -Rows $Rows)"

    # A foreach over no rows runs no iteration, where a pipeline would pass one null through.
    foreach ($change in @($changes.Changed, $changes.Missing, $changes.Added)) {

        foreach ($row in $Rows) {

            if ($row.Change -eq $change) {
                "  $(Format-ModDriftLine -Row $row)"
            }
        }
    }
}
