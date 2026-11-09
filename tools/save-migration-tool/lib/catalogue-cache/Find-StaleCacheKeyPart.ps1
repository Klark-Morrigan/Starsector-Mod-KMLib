# Names the first part of a cached key that differs from the current one, in words for the log,
# or returns null when the entry is current. The code comes first: a change there makes every other part moot.
function Find-StaleCacheKeyPart {

    param(
        # Keys as New-CatalogueCacheKey builds them.
        [object] $CachedKey,
        [object] $CurrentKey
    )

    $partNames = [ordered]@{
        Code = "the catalogue's code"
        Mods = 'the mods'
        DataFiles = 'the data files'
    }

    foreach ($part in $partNames.Keys) {

        if ($CachedKey.$part -cne $CurrentKey.$part) {
            return $partNames[$part]
        }
    }

    return $null
}
