# Says in one line what a load's cause was read as, so the diagnostics show each reading.
function Format-MissingContentLine {

    param([object] $Load)

    $missing = $Load.Missing

    if ($null -eq $missing) {
        return "$($Load.SaveFolder): the cause names no missing content"
    }

    $line = "$($Load.SaveFolder): missing $($missing.Kind) $($missing.Id)"

    if ($missing.SpecClass) {
        $line += " of spec class $($missing.SpecClass)"
    }

    if ($missing.ReportedId -ne $missing.Id) {
        $line += ", named as $($missing.ReportedId)"
    }

    return $line
}
