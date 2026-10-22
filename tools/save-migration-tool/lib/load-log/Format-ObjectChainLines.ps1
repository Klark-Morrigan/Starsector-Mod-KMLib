# Says what a load's object chain was read as, so the diagnostics show each reading: one line for the counts,
# and an indented second one naming the nearest mod class when there is one.
# The nearest mod class is the likeliest holder of the failed object. Classes further out mostly show
# the path XStream took to reach it, since XStream writes an object where it first meets it.
function Format-ObjectChainLines {

    param([object] $Load)

    $chain = @($Load.ObjectChain)
    $modLinks = Select-ModChainLink -ObjectChain $chain
    $save = Format-ConsoleText -Role 'Path' -Text $Load.SaveFolder
    $counts = "${save}: object chain length $(Format-ConsoleNumber $chain.Count), " +
        "mod classes $(Format-ConsoleNumber $modLinks.Count)"

    if ($modLinks.Count -eq 0) {
        return $counts
    }

    $nearest = $modLinks | Sort-Object Depth | Select-Object -First 1
    $class = Format-ConsoleText -Role 'Code' -Text $nearest.Class
    $owners = @($nearest.Owners | ForEach-Object { Format-ConsoleText -Role 'Mod' -Text $_ }) -join ', '

    return @(
        "$counts,"
        "  nearest $class ($owners) at depth $(Format-ConsoleNumber $nearest.Depth)"
    )
}
