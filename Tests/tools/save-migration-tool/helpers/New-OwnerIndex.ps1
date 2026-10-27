# Builds an owner index, the dictionary of ID to owners the content catalogue passes around, from a hashtable.
function New-OwnerIndex {

    param([hashtable] $OwnersById = @{})

    $index = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]]::new()

    foreach ($id in $OwnersById.Keys) {
        $index.Add($id, [System.Collections.Generic.List[string]]::new([string[]] $OwnersById[$id]))
    }

    return $index
}
