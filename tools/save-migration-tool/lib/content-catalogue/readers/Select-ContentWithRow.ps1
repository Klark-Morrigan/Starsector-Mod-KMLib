# Keeps the IDs that both a content file and a spreadsheet row define, as the game requires of hulls and weapons,
# each with the owners of its file and of its row.
function Select-ContentWithRow {

    param(
        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $FileOwners,
        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $RowOwners
    )

    $ownersById = New-ContentOwnerIndex

    foreach ($entry in $FileOwners.GetEnumerator()) {

        $rowOwnersOfId = $null

        if (-not $RowOwners.TryGetValue($entry.Key, [ref] $rowOwnersOfId)) {
            continue
        }

        foreach ($owner in @($entry.Value) + @($rowOwnersOfId)) {
            Add-ContentOwner -OwnersById $ownersById -Id $entry.Key -Owner $owner
        }
    }

    return $ownersById
}
