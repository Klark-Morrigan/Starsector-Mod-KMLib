# Turns one mod ID's winning copy into its roster entry.
# RelativePath is the folder relative to the install root, however the root was written.
# Jars are full paths, each declared JAR resolved against the mod's folder as the game resolves it.
# VersionInfo holds the parts the game compares the version by, against a save's.
# SortString and ReplacedPaths are kept as mod_info.json writes them, paths relative to the mod's folder.
function ConvertTo-RosterEntry {

    param(
        [object] $ModGroup,
        [string] $StarsectorRoot
    )

    $winner = $ModGroup.Winner
    $folder = $winner.Folder.FullName

    $entry = [pscustomobject]@{
        Id              = $winner.Id
        Version         = Format-ModVersion -Version $winner.Version
        VersionInfo     = ConvertTo-VersionInfo -Version $winner.Version
        Name            = $winner.Name
        Folder          = $folder
        RelativePath    = [System.IO.Path]::GetRelativePath($StarsectorRoot, $folder)
        Jars            = @(foreach ($jar in $winner.Jars) { [System.IO.Path]::GetFullPath($jar, $folder) })
        SortString      = $winner.SortString
        ReplacedPaths   = $winner.ReplacedPaths
        Status          = $winner.Status
        SupersededCount = $ModGroup.Superseded.Count
    }

    return Add-ModDefaultDisplay -Mod $entry
}
