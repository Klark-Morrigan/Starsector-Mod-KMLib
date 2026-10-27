# Writes an owner index as one line, IDs sorted and each owner list in its own order: "a=x+y b=z".
# A case then compares one literal instead of walking the dictionary.
function Format-OwnerIndex {

    param([System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $OwnersById)

    $ids = [string[]] $OwnersById.Keys
    [System.Array]::Sort($ids, [System.StringComparer]::Ordinal)

    return ($ids | ForEach-Object { "$_=$($OwnersById[$_] -join '+')" }) -join ' '
}
