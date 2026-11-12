# Builds the key one kind's catalogue is cached under: three hashes, one per thing that decides what it holds.
# Kept apart, so a stale entry can say which of them changed.
function New-CatalogueCacheKey {

    param(
        # The code that builds the catalogue, as Get-CatalogueCodeHash hashes it.
        [string] $CodeHash,

        # The data folders and how their copies combine, as Get-ContentSourcesHash hashes them.
        [string] $SourcesHash,

        # Every data file the kind is read from, and those of the kinds it is built from,
        # as Get-ContentKindInputFiles lists them.
        [string[]] $DataFilePaths
    )

    return [pscustomobject]@{
        Code = $CodeHash
        Mods = $SourcesHash
        DataFiles = Get-FileStampHash -Paths $DataFilePaths
    }
}
