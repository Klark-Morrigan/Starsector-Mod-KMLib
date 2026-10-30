# Records that an owner defines an ID, keeping each owner once per ID.
# IDs compare case-sensitively, as the game's spec stores look them up.
function Add-ContentOwner {

    param(
        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $OwnersById,
        [string] $Id,
        [string] $Owner
    )

    $owners = $null

    if (-not $OwnersById.TryGetValue($Id, [ref] $owners)) {

        $owners = [System.Collections.Generic.List[string]]::new()
        $OwnersById.Add($Id, $owners)
    }

    if (-not $owners.Contains($Owner)) {
        $owners.Add($Owner)
    }
}
