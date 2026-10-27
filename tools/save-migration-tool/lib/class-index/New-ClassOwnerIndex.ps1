# Finds which owners' JARs hold each of the given classes, so a class a failure names resolves to the mods behind it.
# A source is an owner and its JARs: a mod ID with the JARs its mod_info.json declares, or the core owner.
# A mod can bundle another mod's classes, so a class keeps every mod whose JARs hold it, in source order.
# A JAR that is not on disk, or that no zip reader opens, is skipped and returned with the reason.
# The JARs read are counted apart for the mods and for the core, so a caller reports each against what it listed.
function New-ClassOwnerIndex {

    param(
        [object[]] $Sources,
        [string[]] $ClassNames
    )

    $coreOwner = Get-CoreOwnerName
    $idComparer = Get-ModIdComparer
    $ownersByClass = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]]::new()
    $skippedJars = [System.Collections.Generic.List[object]]::new()
    $modJarCount = 0
    $coreJarCount = 0

    foreach ($source in $Sources) {
        foreach ($jar in $source.Jars) {

            if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {

                $skippedJars.Add([pscustomobject]@{ Path = $jar; Reason = 'not on disk' })
                continue
            }

            try {
                $heldClasses = @(Select-JarClassNames -Path $jar -ClassNames $ClassNames)

            } catch {

                # A broken JAR in one mod must not end the diagnosis of every other.
                $skippedJars.Add([pscustomobject]@{ Path = $jar; Reason = $_.Exception.Message })
                continue
            }

            if ($idComparer.Equals($source.Owner, $coreOwner)) {
                $coreJarCount++

            } else {
                $modJarCount++
            }

            foreach ($className in $heldClasses) {

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

    $shadowedClassCount = 0
    $sharedClassCount = 0

    foreach ($owners in $ownersByClass.Values) {

        if ($owners.Count -lt 2) {
            continue
        }

        # Every mod JAR loads through one URLClassLoader whose parent is the game's (ScriptStore).
        # It asks its parent first, so a mod's copy of a core class never loads, and the core stays the only owner.
        if ($owners.Contains($coreOwner)) {

            $shadowedClassCount++
            $owners.Clear()
            $owners.Add($coreOwner)

        } else {
            $sharedClassCount++
        }
    }

    return [pscustomobject]@{
        OwnersByClass      = $ownersByClass
        ModJarCount        = $modJarCount
        CoreJarCount       = $coreJarCount
        SharedClassCount   = $sharedClassCount
        ShadowedClassCount = $shadowedClassCount
        SkippedJars        = $skippedJars.ToArray()
    }
}
