# Turns one mod id's winning copy into its roster entry.
# RelativePath is the folder relative to the install root, however the root was written.
#   Depends on Format-ModVersion being dot-sourced into the same scope.
function ConvertTo-RosterEntry {

    param(
        [object] $ModGroup,
        [string] $StarsectorRoot
    )

    $winner = $ModGroup.Winner

    return [pscustomobject]@{
        Id              = $winner.Id
        Version         = Format-ModVersion -Version $winner.Version
        Folder          = $winner.Folder.FullName
        RelativePath    = [System.IO.Path]::GetRelativePath($StarsectorRoot, $winner.Folder.FullName)
        Status          = $winner.Status
        SupersededCount = $ModGroup.Superseded.Count
    }
}
