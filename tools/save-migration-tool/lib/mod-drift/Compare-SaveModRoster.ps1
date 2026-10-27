# Pairs a save's mods with the installed roster by mod ID, one drift row per mod on either side:
# the save's mods in the save's order, then the installed mods the save lacks, in the roster's order.
# IDs match case-sensitively, as the game matches them.
function Compare-SaveModRoster {

    param(
        [object[]] $SavedMods,
        [object[]] $Roster
    )

    $installedById = [System.Collections.Generic.Dictionary[string, object]]::new([System.StringComparer]::Ordinal)
    $savedIds = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)

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
