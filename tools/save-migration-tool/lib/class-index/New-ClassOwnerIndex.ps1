# Indexes which owners' jars hold each class, so a class a failure names resolves to the mods behind it.
# A source is an owner and its jars: a mod id with the jars its mod_info.json declares, or the core owner.
# A mod can bundle another's classes, so a class keeps every owner whose jars hold it, in source order.
# A declared jar that is not on disk is skipped and returned in MissingJars.
#   Depends on Read-JarClassNames being dot-sourced into the same scope.
function New-ClassOwnerIndex {

    param([object[]] $Sources)

    $ownersByClass = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]]::new()
    $missingJars = [System.Collections.Generic.List[string]]::new()
    $jarCount = 0

    foreach ($source in $Sources) {
        foreach ($jar in $source.Jars) {

            if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {

                $missingJars.Add($jar)
                continue
            }

            $jarCount++

            foreach ($className in Read-JarClassNames -Path $jar) {

                $owners = $null

                if (-not $ownersByClass.TryGetValue($className, [ref] $owners)) {

                    $owners = [System.Collections.Generic.List[string]]::new()
                    $ownersByClass.Add($className, $owners)
                }

                if (-not $owners.Contains($source.Owner)) {
                    $owners.Add($source.Owner)
                }
            }
        }
    }

    $sharedClassCount = 0

    foreach ($owners in $ownersByClass.Values) {

        if ($owners.Count -gt 1) {
            $sharedClassCount++
        }
    }

    return [pscustomobject]@{
        OwnersByClass    = $ownersByClass
        JarCount         = $jarCount
        SharedClassCount = $sharedClassCount
        MissingJars      = $missingJars.ToArray()
    }
}
