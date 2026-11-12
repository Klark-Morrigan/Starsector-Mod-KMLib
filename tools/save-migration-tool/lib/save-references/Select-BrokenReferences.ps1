# Keeps the references whose ID the catalogue lacks for their kind: content the save names and no installed mod
# defines any more. IDs compare case-sensitively, as the game looks them up.
# Returns one row per kind and ID, kinds in the catalogue's order, then IDs code unit by code unit,
# each with how many times the save names it, in all and per site, the busiest site first.
function Select-BrokenReferences {

    param(
        # The references, as Read-SaveReferences returns them.
        [object[]] $References,

        # Each kind's owner index, as Read-ContentCatalogue returns them, holding every kind the references name.
        [System.Collections.IDictionary] $OwnersByKind
    )

    $brokenByKind = @{}

    foreach ($reference in $References) {

        if ($OwnersByKind[$reference.Kind].ContainsKey($reference.Id)) {
            continue
        }

        if (-not $brokenByKind.ContainsKey($reference.Kind)) {
            $brokenByKind[$reference.Kind] = [System.Collections.Generic.List[object]]::new()
        }

        $brokenByKind[$reference.Kind].Add($reference)
    }

    $idSelector = [System.Func[object, string]] {

        param($reference)

        return $reference.Id
    }

    return @(foreach ($kind in $OwnersByKind.Keys) {

        if (-not $brokenByKind.ContainsKey($kind)) {
            continue
        }

        $references = [System.Linq.Enumerable]::OrderBy(
            [object[]] $brokenByKind[$kind].ToArray(),
            $idSelector,
            [System.StringComparer]::Ordinal)

        foreach ($reference in $references) {

            $sites = [ordered]@{}

            $busiestSites = $reference.UseCountBySite.GetEnumerator() |
                Sort-Object -Property @{ Expression = 'Value'; Descending = $true }, 'Key'

            foreach ($siteUses in $busiestSites) {
                $sites[$siteUses.Key] = $siteUses.Value
            }

            [pscustomobject]@{
                Kind = $kind
                Id = $reference.Id
                UseCount = $reference.UseCount
                Sites = $sites
            }
        }
    })
}
