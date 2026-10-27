# Puts the owners of every ID in the order the game searches their sources: the mods in load order, then the game.
# Owners are found file by file and part by part, so the order they were found in says nothing to a reader.
function Set-ContentOwnerOrder {

    param(
        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $OwnersById,

        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources
    )

    $rankByOwner = [System.Collections.Generic.Dictionary[string, int]]::new()

    for ($rank = 0; $rank -lt $Sources.Count; $rank++) {
        $rankByOwner[$Sources[$rank].Owner] = $rank
    }

    $compareRanks = [System.Comparison[string]] {

        param($first, $second)

        return $rankByOwner[$first].CompareTo($rankByOwner[$second])
    }

    foreach ($owners in $OwnersById.Values) {

        if ($owners.Count -gt 1) {
            $owners.Sort($compareRanks)
        }
    }
}
