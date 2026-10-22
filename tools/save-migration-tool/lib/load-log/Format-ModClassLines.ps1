# Writes each class of an object chain that a mod owns, innermost first, with its depth and owners.
# Writes nothing for a chain holding no mod class.
function Format-ModClassLines {

    param([object[]] $ObjectChain)

    $modLinks = Select-ModChainLink -ObjectChain $ObjectChain

    if ($modLinks.Count -eq 0) {
        return
    }

    'Mod classes in the object chain, innermost first:'

    foreach ($link in $modLinks) {

        $class = Format-ConsoleText -Role 'Code' -Text $link.Class

        "  depth $(Format-ConsoleNumber $link.Depth): $class ($(Format-OwnerList -Owners $link.Owners))"
    }
}
