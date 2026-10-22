# Adds to each class in the object chains of a load's attempts the owners whose JARs hold it.
# A class no indexed JAR holds gets no owners: one of the Java runtime's, or one no installed mod ships.
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

    return $Load
}
