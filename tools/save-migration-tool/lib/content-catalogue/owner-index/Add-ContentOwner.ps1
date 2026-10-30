# Records that an owner defines an ID, keeping each owner once per ID.
# IDs compare case-sensitively, as the game's spec stores look them up.
#
# A catalogue run adds hundreds of thousands of owners, and the indexer costs a fraction of a TryGetValue call,
# as PowerShell answers a missing key with null.
function Add-ContentOwner {

    param(
        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $OwnersById,
        [string] $Id,
        [string] $Owner
    )

    $owners = $OwnersById[$Id]

    if ($null -eq $owners) {

        $owners = [System.Collections.Generic.List[string]]::new()
        $OwnersById[$Id] = $owners

    } elseif ($owners.Contains($Owner)) {
        return
    }

    $owners.Add($Owner)
}
