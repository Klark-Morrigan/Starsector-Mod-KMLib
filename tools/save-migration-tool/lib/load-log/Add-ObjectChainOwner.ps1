# Adds to each class in a load's object chains the owners whose jars hold it,
# and gives the load its first attempt's chain, since that attempt read campaign.xml, the newest file.
# A class no indexed jar holds gets no owners: one of the Java runtime's, or one no installed mod ships.
function Add-ObjectChainOwner {

    param(
        [object] $Load,

        # Class name to its owners, as New-ClassOwnerIndex builds it.
        [object] $OwnersByClass
    )

    foreach ($attempt in $Load.Attempts) {

        $attempt.ObjectChain = @(foreach ($link in $attempt.ObjectChain) {

            # Assigned apart from the object, since an if in value position unrolls an array to a scalar or null.
            $owners = @()

            if ($OwnersByClass.ContainsKey($link.Class)) {
                $owners = @($OwnersByClass[$link.Class])
            }

            [pscustomobject]@{
                Depth  = $link.Depth
                Class  = $link.Class
                Owners = $owners
            }
        })
    }

    $Load | Add-Member -NotePropertyName 'ObjectChain' -NotePropertyValue $Load.Attempts[0].ObjectChain

    return $Load
}
