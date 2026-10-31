# Gives an object the columns PowerShell prints for it on a console, in the given order.
# PowerShell lays an object of more than four fields out as a list; the chosen columns keep it a table.
# Five or more columns still print as a list, so a row that needs them takes a view under formats\ instead.
# The object keeps every field for a caller that selects or serialises it.
function Add-DefaultDisplay {

    param(
        [object] $InputObject,
        [string[]] $PropertyNames
    )

    $columns = [System.Management.Automation.PSPropertySet]::new('DefaultDisplayPropertySet', $PropertyNames)

    $InputObject | Add-Member -MemberType MemberSet -Name 'PSStandardMembers' -Force `
        -Value ([System.Management.Automation.PSMemberInfo[]] @($columns))

    return $InputObject
}
