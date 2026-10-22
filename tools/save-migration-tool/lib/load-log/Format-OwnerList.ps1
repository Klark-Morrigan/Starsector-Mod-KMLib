# Writes a class's owners as one comma-separated run, each painted as a mod.
function Format-OwnerList {

    param([string[]] $Owners)

    return @($Owners | ForEach-Object { Format-ConsoleText -Role 'Mod' -Text $_ }) -join ', '
}
