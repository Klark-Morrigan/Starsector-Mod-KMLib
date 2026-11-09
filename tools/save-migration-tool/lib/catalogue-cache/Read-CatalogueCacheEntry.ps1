# Reads the key one kind's cache entry was written under, and how many of its IDs the game builds at startup.
# Returns null when the kind has no entry; throws when the entry does not parse.
# The owners are read apart, by Read-CatalogueCacheOwners, only once the key matches.
function Read-CatalogueCacheEntry {

    param(
        # The install's cache folder, as Get-InstallCacheFolder names it.
        [string] $CacheFolder,

        [string] $Kind
    )

    $fileNames = Get-CatalogueCacheFileNames `
        -Kind $Kind

    $keyPath = Join-Path $CacheFolder $fileNames.Folder `
        -AdditionalChildPath $fileNames.Key

    $ownersPath = Join-Path $CacheFolder $fileNames.Folder `
        -AdditionalChildPath $fileNames.Owners

    if (-not [System.IO.File]::Exists($keyPath) `
            -or -not [System.IO.File]::Exists($ownersPath)) {

        return $null
    }

    $entry = [System.IO.File]::ReadAllText($keyPath) |
        ConvertFrom-Json

    return [pscustomobject]@{

        Key = [pscustomobject]@{
            Code = $entry.Code;
            Mods = $entry.Mods;
            DataFiles = $entry.DataFiles }

        GeneratedCount = [int] $entry.GeneratedCount
    }
}
