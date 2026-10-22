# Finds which owners' jars hold each of the given classes, so a class a failure names resolves to the mods behind it.
# A source is an owner and its JARs: a mod ID with the JARs its mod_info.json declares, or the core owner.
# A mod can bundle another mod's classes, so a class keeps every mod whose jars hold it, in source order.
# A jar that is not on disk, or that no zip reader opens, is skipped and returned with the reason.
function New-ClassOwnerIndex {

    param(
        [object[]] $Sources,
        [string[]] $ClassNames
    )

    $coreOwner = Get-CoreOwnerName
    $wantedClasses = [System.Collections.Generic.HashSet[string]]::new($ClassNames, [System.StringComparer]::Ordinal)
    $ownersByClass = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]]::new()
    $skippedJars = [System.Collections.Generic.List[object]]::new()
    $jarCount = 0

    foreach ($source in $Sources) {
        foreach ($jar in $source.Jars) {

            if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {

                $skippedJars.Add([pscustomobject]@{ Path = $jar; Reason = 'not on disk' })
                continue
            }

            try {
                $heldClasses = [System.Collections.Generic.HashSet[string]]::new(
                    [string[]] @(Read-JarClassNames -Path $jar), [System.StringComparer]::Ordinal)

            } catch {

                # A broken jar in one mod must not end the diagnosis of every other.
                $skippedJars.Add([pscustomobject]@{ Path = $jar; Reason = $_.Exception.Message })
                continue
            }

            $jarCount++
            $heldClasses.IntersectWith($wantedClasses)

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

        # Every mod jar loads through one URLClassLoader whose parent is the game's (ScriptStore).
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
        JarCount           = $jarCount
        SharedClassCount   = $sharedClassCount
        ShadowedClassCount = $shadowedClassCount
        SkippedJars        = $skippedJars.ToArray()
    }
}
