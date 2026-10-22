# Gives a mod object the columns PowerShell prints for it on a console: ID, version, name.
# The installed mods and a save's mods then print as the same table, so the two lists read side by side.
# The object keeps every field for a caller that selects or serialises it.
function Add-ModDefaultDisplay {

    param([object] $Mod)

    $columns = [System.Management.Automation.PSPropertySet]::new(
        'DefaultDisplayPropertySet',
        [string[]] @('Id', 'Version', 'Name'))

    $Mod | Add-Member -MemberType MemberSet -Name 'PSStandardMembers' -Force `
        -Value ([System.Management.Automation.PSMemberInfo[]] @($columns))

    return $Mod
}
