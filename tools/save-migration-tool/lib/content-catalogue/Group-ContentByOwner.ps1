# Regroups catalogue rows by owner: one entry per source that defines anything, in the game's search order,
# holding its IDs by kind, each kind and its IDs in the rows' order.
# An ID several owners define sits under each of them.
# On a console an entry prints as a table row of its owner, version, name and ID count.
function Group-ContentByOwner {

    param(
        # Catalogue rows: Kind, Id and Owners.
        [object[]] $Rows,

        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources
    )

    # Owners are mod IDs, which the game tells apart case-sensitively, so no hashtable.
    $idsByKindByOwner = [System.Collections.Generic.Dictionary[string, object]]::new()

    foreach ($row in $Rows) {
        foreach ($owner in $row.Owners) {

            $idsByKind = $null

            if (-not $idsByKindByOwner.TryGetValue($owner, [ref] $idsByKind)) {

                $idsByKind = [ordered]@{}
                $idsByKindByOwner.Add($owner, $idsByKind)
            }

            if (-not $idsByKind.Contains($row.Kind)) {
                $idsByKind[$row.Kind] = [System.Collections.Generic.List[string]]::new()
            }

            $idsByKind[$row.Kind].Add($row.Id)
        }
    }

    return @(foreach ($source in $Sources) {

        $idsByKind = $null

        if (-not $idsByKindByOwner.TryGetValue($source.Owner, [ref] $idsByKind)) {
            continue
        }

        $idCount = 0
        $idArraysByKind = [ordered]@{}

        foreach ($kind in $idsByKind.Keys) {

            $idArraysByKind[$kind] = $idsByKind[$kind].ToArray()
            $idCount += $idsByKind[$kind].Count
        }

        $group = [pscustomobject]@{
            Owner   = $source.Owner
            Name    = $source.Name
            Version = $source.Version
            IdCount = $idCount
            Ids     = $idArraysByKind
        }

        Add-DefaultDisplay -InputObject $group -PropertyNames @('Owner', 'Version', 'Name', 'IdCount')
    })
}
