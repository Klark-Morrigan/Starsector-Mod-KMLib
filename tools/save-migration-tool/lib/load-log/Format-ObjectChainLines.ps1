# Says what a load's object chain was read as: one line for the counts,
# and an indented second one naming the nearest mod class when there is one. The caller names the load.
# The nearest mod class is the likeliest holder of the failed object. Classes further out mostly show
# the path XStream took to reach it, since XStream writes an object where it first meets it.
function Format-ObjectChainLines {

    param([object] $Load)

    $chain = @($Load.ObjectChain)
    $modLinks = Select-ModChainLink -ObjectChain $chain
    $counts = "object chain length $(Format-ConsoleNumber $chain.Count), " +
        "mod classes $(Format-ConsoleNumber $modLinks.Count)"

    if ($modLinks.Count -eq 0) {
        return $counts
    }

    # The chain runs innermost first, so the first mod link is the nearest.
    $nearest = $modLinks[0]
    $class = Format-ConsoleText -Role 'Code' -Text $nearest.Class
    $owners = Format-ConsoleList -Role 'Mod' -Values $nearest.Owners

    return @(
        "$counts,"
        "  nearest $class ($owners) at depth $(Format-ConsoleNumber $nearest.Depth)"
    )
}
