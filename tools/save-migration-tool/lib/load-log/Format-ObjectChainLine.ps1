# Says in one line what a load's object chain was read as, so the diagnostics show each reading.
# The nearest mod class is the likeliest holder of the failed object. Classes further out mostly show
# the path XStream took to reach it, since XStream writes an object where it first meets it.
#   Depends on Get-CoreOwnerName being dot-sourced into the same scope.
function Format-ObjectChainLine {

    param([object] $Load)

    $coreOwner = Get-CoreOwnerName
    $chain = @($Load.ObjectChain)

    # Every mod jar loads through one URLClassLoader whose parent is the game's (ScriptStore).
    # It asks its parent first, so a mod's copy of a core class never loads.
    $modLinks = @($chain | Where-Object { $_.Owners.Count -gt 0 -and $_.Owners -notcontains $coreOwner })

    $line = "$($Load.SaveFolder): object chain length $($chain.Count), mod classes $($modLinks.Count)"

    if ($modLinks.Count -eq 0) {
        return $line
    }

    $nearest = $modLinks | Sort-Object Depth | Select-Object -First 1

    return "$line, nearest $($nearest.Class) ($($nearest.Owners -join ', ')) at depth $($nearest.Depth)"
}
