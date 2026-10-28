# Paints each value of a list in one role's colour, in order, and joins them into one comma-separated run.
function Format-ConsoleList {

    param(
        [string] $Role,
        [string[]] $Values
    )

    # A foreach over a missing list runs no iteration, where a pipeline would paint one empty value.
    $paintedValues = @(foreach ($value in $Values) { Format-ConsoleText -Role $Role -Text $value })

    return $paintedValues -join ', '
}
