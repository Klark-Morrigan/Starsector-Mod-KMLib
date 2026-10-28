# Counts, and names, the mods a save was written with once and not at its last save.
# Their content can still sit in the save, though the game's load list compares only the last save's mods.
function Format-DroppedModSummary {

    param(
        [object[]] $SavedMods,
        [object[]] $EverEnabledMods
    )

    # A foreach over a missing list runs no iteration, where a pipeline would pass one null through.
    $savedIds = @(foreach ($mod in $SavedMods) { $mod.Id })
    $droppedIds = @(foreach ($mod in $EverEnabledMods) { if ($mod.Id -cnotin $savedIds) { $mod.Id } })
    $summary = "$(Format-ConsoleNumber $droppedIds.Count) more were enabled in an earlier save and are not now"

    if ($droppedIds.Count -eq 0) {
        return $summary
    }

    return "${summary}: $(Format-ConsoleList -Role 'Mod' -Values $droppedIds)"
}
