# A hash of the data folders the catalogue reads and what decides how their copies combine, for the cache key:
# each source's owner, version and folder, whether it is a mod, the paths it replaces, and the search order.
function Get-ContentSourcesHash {

    param(
        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources
    )

    $sourceLines = foreach ($source in $Sources) {
        "$($source.Owner)|$($source.Version)|$($source.Folder)|$($source.IsMod)|$($source.ReplacedPaths -join '|')"
    }

    return Get-TextHash -Text ($sourceLines -join "`n")
}
