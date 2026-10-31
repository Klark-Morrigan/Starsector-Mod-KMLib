# Pairs a save's mods with the installed roster by mod ID, one drift row per mod on either side:
# the save's mods in the save's order, then the installed mods the save lacks, in the roster's order.
function Compare-SaveModRoster {

    param(
        [object[]] $SavedMods,
        [object[]] $Roster
    )

    $idComparer = Get-ModIdComparer
    $installedById = [System.Collections.Generic.Dictionary[string, object]]::new($idComparer)
    $savedIds = [System.Collections.Generic.HashSet[string]]::new($idComparer)

    foreach ($installed in $Roster) {
        $installedById[$installed.Id] = $installed
    }

    foreach ($saved in $SavedMods) {

        [void] $savedIds.Add($saved.Id)
        $installed = $null
        [void] $installedById.TryGetValue($saved.Id, [ref] $installed)

        New-ModDriftRow -Saved $saved -Installed $installed
    }

    foreach ($installed in $Roster) {

        if (-not $savedIds.Contains($installed.Id)) {
            New-ModDriftRow -Saved $null -Installed $installed
        }
    }
}
