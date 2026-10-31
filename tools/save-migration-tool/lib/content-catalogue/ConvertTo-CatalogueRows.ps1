# Lists the catalogue as one row per defined ID: Kind, Id and Owners.
# Kinds keep the catalogue's order, and each kind's IDs sort code unit by code unit,
# so two runs over the same install list the same rows in the same order.
function ConvertTo-CatalogueRows {

    param(
        # Each kind's owner index, as New-ContentCatalogue builds them, in the order the kinds list.
        [System.Collections.IDictionary] $OwnersByKind,

        # Wildcard patterns an ID must match one of, ignoring case. Every ID when empty.
        [string[]] $IdPatterns
    )

    $patterns = @(foreach ($pattern in $IdPatterns) { [WildcardPattern]::new($pattern, 'IgnoreCase') })

    return @(foreach ($kindName in $OwnersByKind.Keys) {

        $ownersById = $OwnersByKind[$kindName]
        $ids = [string[]] $ownersById.Keys
        [System.Array]::Sort($ids, [System.StringComparer]::Ordinal)

        foreach ($definedId in $ids) {

            $isListed = $patterns.Count -eq 0

            foreach ($pattern in $patterns) {

                if ($pattern.IsMatch($definedId)) {

                    $isListed = $true
                    break
                }
            }

            if ($isListed) {
                [pscustomobject]@{ Kind = $kindName; Id = $definedId; Owners = $ownersById[$definedId].ToArray() }
            }
        }
    })
}
