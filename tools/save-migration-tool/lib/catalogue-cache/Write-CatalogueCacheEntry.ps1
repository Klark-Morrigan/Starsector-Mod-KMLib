# Writes one kind's catalogue to the cache: its owner index, then the key it was read under
# and how many of its IDs the game builds at startup.
function Write-CatalogueCacheEntry {

    param(
        # The install's cache folder, as Get-InstallCacheFolder names it.
        [string] $CacheFolder,

        [string] $Kind,

        # The key the kind was read under, as New-CatalogueCacheKey builds it.
        [object] $Key,

        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $OwnersById,

        [int] $GeneratedCount
    )

    $fileNames = Get-CatalogueCacheFileNames -Kind $Kind
    $entryFolder = Join-Path $CacheFolder $fileNames.Folder

    New-Item `
        -ItemType Directory `
        -Path $entryFolder `
        -Force |
        Out-Null

    $ownersJson = [System.Text.Json.JsonSerializer]::Serialize(
        $OwnersById,
        $OwnersById.GetType())

    Write-TextFile `
        -Path (Join-Path $entryFolder $fileNames.Owners) `
        -Text $ownersJson |
        Out-Null

    $keyJson = ConvertTo-Json -InputObject ([pscustomobject]@{
        Code = $Key.Code
        Mods = $Key.Mods
        DataFiles = $Key.DataFiles
        GeneratedCount = $GeneratedCount
    })

    Write-TextFile `
        -Path (Join-Path $entryFolder $fileNames.Key) `
        -Text $keyJson |
        Out-Null
}
