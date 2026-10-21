# Turns one mod id's winning copy into its roster entry.
# RelativePath is the folder relative to the install root, however the root was written.
# Jars are full paths, each declared jar resolved against the mod's folder as the game resolves it.
#   Depends on Format-ModVersion being dot-sourced into the same scope.
function ConvertTo-RosterEntry {

    param(
        [object] $ModGroup,
        [string] $StarsectorRoot
    )

    $winner = $ModGroup.Winner
    $folder = $winner.Folder.FullName

    return [pscustomobject]@{
        Id              = $winner.Id
        Version         = Format-ModVersion -Version $winner.Version
        Folder          = $folder
        RelativePath    = [System.IO.Path]::GetRelativePath($StarsectorRoot, $folder)
        Jars            = @(foreach ($jar in $winner.Jars) { [System.IO.Path]::GetFullPath($jar, $folder) })
        Status          = $winner.Status
        SupersededCount = $ModGroup.Superseded.Count
    }
}
