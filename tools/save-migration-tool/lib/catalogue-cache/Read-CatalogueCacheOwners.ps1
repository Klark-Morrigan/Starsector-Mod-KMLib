# Reads one kind's owner index from the cache, as Write-CatalogueCacheEntry wrote it.
# A kind holds up to tens of thousands of IDs, and the typed JSON reader builds the index in one call,
# where filling it from ConvertFrom-Json's objects would take one call per ID.
function Read-CatalogueCacheOwners {

    param(
        # The install's cache folder, as Get-InstallCacheFolder names it.
        [string] $CacheFolder,

        [string] $Kind
    )

    $fileNames = Get-CatalogueCacheFileNames -Kind $Kind

    $ownersPath = Join-Path $CacheFolder $fileNames.Folder `
        -AdditionalChildPath $fileNames.Owners

    $indexType = (New-ContentOwnerIndex).GetType()

    return [System.Text.Json.JsonSerializer]::Deserialize(
        [System.IO.File]::ReadAllText($ownersPath),
        $indexType)
}
