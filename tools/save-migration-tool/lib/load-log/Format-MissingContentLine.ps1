# Says in one line what a load's cause was read as, so the diagnostics show each reading.
function Format-MissingContentLine {

    param([object] $Load)

    $missing = $Load.Missing
    $save = Format-ConsoleText -Role 'Path' -Text $Load.SaveFolder

    if ($null -eq $missing) {
        return "${save}: the cause names no missing content"
    }

    $line = "${save}: missing $($missing.Kind) $(Format-ConsoleText -Role 'Content' -Text $missing.Id)"

    if ($missing.SpecClass) {
        $line += " of spec class $(Format-ConsoleText -Role 'Code' -Text $missing.SpecClass)"
    }

    if ($missing.ReportedId -ne $missing.Id) {
        $line += ", named as $(Format-ConsoleText -Role 'Content' -Text $missing.ReportedId)"
    }

    return $line
}
