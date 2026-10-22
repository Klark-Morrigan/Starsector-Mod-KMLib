# Says in one line what a load's cause was read as. The caller names the load the line belongs to.
function Format-MissingContentLine {

    param([object] $Load)

    $missing = $Load.Missing

    if ($null -eq $missing) {
        return 'the cause names no missing content'
    }

    $line = "missing $($missing.Kind) $(Format-ConsoleText -Role 'Content' -Text $missing.Id)"

    if ($missing.SpecClass) {
        $line += " of spec class $(Format-ConsoleText -Role 'Code' -Text $missing.SpecClass)"
    }

    if ($missing.ReportedId -ne $missing.Id) {
        $line += ", named as $(Format-ConsoleText -Role 'Content' -Text $missing.ReportedId)"
    }

    return $line
}
