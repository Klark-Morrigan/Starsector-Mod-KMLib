# Orders the copies of one content file the way the game merges them, key by key, as it merges any data JSON.
# The game's own copy is the base, and each mod's copy is laid over it in search order,
# so a later copy's key wins over an earlier one's.
# A mod that lists the file's path under "replace" stands alone: the game reads only its copy,
# the first such mod's in search order. It takes the path written with either slash.
function Select-MergedCopies {

    param(
        # One file of Find-ContentFiles: its path under a data folder and its copies, in search order.
        [object] $File
    )

    $backslashPath = $File.RelativePath.Replace('/', '\')
    $coreCopies = [System.Collections.Generic.List[object]]::new()
    $modCopies = [System.Collections.Generic.List[object]]::new()

    foreach ($copy in $File.Copies) {

        if (-not $copy.Source.IsMod) {

            $coreCopies.Add($copy)
            continue
        }

        $isReplacing = $copy.Source.ReplacedPaths -ccontains $File.RelativePath -or
            $copy.Source.ReplacedPaths -ccontains $backslashPath

        if ($isReplacing) {
            return @($copy)
        }

        $modCopies.Add($copy)
    }

    return $coreCopies.ToArray() + $modCopies.ToArray()
}
