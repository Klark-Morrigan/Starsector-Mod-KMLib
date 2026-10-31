# Gives a mod object the columns PowerShell prints for it on a console: ID, version, name.
# The installed mods and a save's mods then print as the same table, so the two lists read side by side.
function Add-ModDefaultDisplay {

    param([object] $Mod)

    return Add-DefaultDisplay -InputObject $Mod -PropertyNames @('Id', 'Version', 'Name')
}
