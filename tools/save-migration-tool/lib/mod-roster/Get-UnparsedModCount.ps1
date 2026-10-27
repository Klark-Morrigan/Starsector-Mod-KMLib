# Counts the roster's mods whose mod_info.json gave no ID. Their JARs are unknown, so their classes own nothing.
function Get-UnparsedModCount {

    param([object[]] $Roster)

    $parsedStatus = (Get-ModStatusNames).Parsed

    # A foreach over no roster runs no iteration, where a pipeline would pass one null through.
    return @(foreach ($mod in $Roster) { if ($mod.Status -ne $parsedStatus) { $mod } }).Count
}
