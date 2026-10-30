# Orders the copies of one content file the way the game merges them, key by key, as it merges any data JSON.
# The game's own copy is the base, and each mod's copy is laid over it in search order,
# so a later copy's key wins over an earlier one's.
# A mod that lists the file's path under "replace" stands alone: the game reads only its copy,
# as Find-ReplacingSource picks it, taking the path written with either slash.
# Returns the copies in merge order, and that replacement's path and mod, or null when no mod replaces the file.
function Select-MergedCopies {

    param(
        # One file of Find-ContentFiles: its path under a data folder and its copies, in search order.
        [object] $File
    )

    $sources = @(foreach ($copy in $File.Copies) { $copy.Source })
    $replacingSource = Find-ReplacingSource -Sources $sources -Path $File.RelativePath -AcceptsEitherSlash
    $coreCopies = [System.Collections.Generic.List[object]]::new()
    $modCopies = [System.Collections.Generic.List[object]]::new()

    foreach ($copy in $File.Copies) {

        if ([object]::ReferenceEquals($copy.Source, $replacingSource)) {

            $replacement = [pscustomobject]@{ Path = $File.RelativePath; Owner = $replacingSource.Owner }

            return [pscustomobject]@{ Copies = @($copy); Replacement = $replacement }
        }

        if ($copy.Source.IsMod) {
            $modCopies.Add($copy)
        } else {
            $coreCopies.Add($copy)
        }
    }

    return [pscustomobject]@{ Copies = $coreCopies.ToArray() + $modCopies.ToArray(); Replacement = $null }
}
