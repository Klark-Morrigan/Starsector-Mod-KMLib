# Returns an owner index holding every ID of both, each with the owners either gives it.
# Merging copies entry by entry, and most merges start from an empty index,
# so an empty index gives way to the other, which is returned as it is.
# An ID only the other holds takes a copy of its owners in one call, instead of one call per owner.
function Merge-ContentOwners {

    param(
        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $Into,
        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $From
    )

    if ($Into.Count -eq 0) {
        return $From
    }

    foreach ($entry in $From.GetEnumerator()) {

        if ($null -eq $Into[$entry.Key]) {

            $Into[$entry.Key] = [System.Collections.Generic.List[string]]::new($entry.Value)
            continue
        }

        foreach ($owner in $entry.Value) {
            Add-ContentOwner -OwnersById $Into -Id $entry.Key -Owner $owner
        }
    }

    return $Into
}
