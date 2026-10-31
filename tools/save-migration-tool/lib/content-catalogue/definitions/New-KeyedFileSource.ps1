# Describes one JSON file whose top-level keys are the IDs it defines, as planets.json and terrain.json are.
# The game merges every source's copy of the file, so the IDs are the keys any copy holds.
function New-KeyedFileSource {

    param(
        # The file's path under a data folder, as mod_info.json's "replace" names it.
        [string] $Path,

        # The key of an object whose own keys are the IDs, instead of the file's top level.
        [string] $NestedKey,

        # Keys that are not IDs.
        [string[]] $ExcludedKeys = @(),

        # Only a key whose value is an object defines an ID; the game skips any other.
        [switch] $IsObjectEntryOnly
    )

    return [pscustomobject]@{
        Path              = $Path
        NestedKey         = $NestedKey
        ExcludedKeys      = $ExcludedKeys
        IsObjectEntryOnly = [bool] $IsObjectEntryOnly
    }
}
