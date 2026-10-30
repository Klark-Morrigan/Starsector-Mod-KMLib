# Reads the IDs a JSON file keyed by ID defines across the sources, with each source whose copy holds the key.
# The game merges every copy key by key, so the IDs are the keys any copy holds,
# unless a mod replaces the file, which leaves only that mod's copy.
# Where only object entries define an ID, the value the merge keeps decides: the last copy's in merge order.
# A copy that does not parse is skipped and returned with the reason,
# and a mod that replaces the file is returned with the file's path, or null when none does.
function Read-KeyedFileOwners {

    param(
        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources,

        # The file, as New-KeyedFileSource describes it.
        [object] $KeyedFile
    )

    $keyOwners = New-ContentOwnerIndex
    $isObjectByKey = [System.Collections.Generic.Dictionary[string, bool]]::new()
    $skippedFiles = [System.Collections.Generic.List[object]]::new()
    $file = @(Find-ListedContentFiles -Sources $Sources -RelativePaths @($KeyedFile.Path))
    $copyCount = 0

    $merge = if ($file.Count -eq 0) {
        [pscustomobject]@{ Copies = @(); Replacement = $null }
    } else {
        Select-MergedCopies -File $file[0]
    }

    foreach ($copy in $merge.Copies) {

        $copyCount++

        try {
            $json = ConvertFrom-StarsectorJson -Path $copy.Path

        } catch {

            $skippedFiles.Add([pscustomobject]@{ Path = $copy.Path; Reason = $_.Exception.Message })
            continue
        }

        $entries = if ($KeyedFile.NestedKey) {
            Get-JsonPropertyValue -Object $json -Name $KeyedFile.NestedKey
        } else {
            $json
        }

        if ($entries -isnot [System.Collections.IDictionary]) {
            continue
        }

        foreach ($key in $entries.Keys) {

            if ($KeyedFile.ExcludedKeys -ccontains $key) {
                continue
            }

            $isObjectByKey[$key] = $entries[$key] -is [System.Collections.IDictionary]
            Add-ContentOwner -OwnersById $keyOwners -Id $key -Owner $copy.Source.Owner
        }
    }

    $ownersById = New-ContentOwnerIndex

    foreach ($entry in $keyOwners.GetEnumerator()) {

        if (-not $KeyedFile.IsObjectEntryOnly -or $isObjectByKey[$entry.Key]) {
            $ownersById.Add($entry.Key, $entry.Value)
        }
    }

    return [pscustomobject]@{
        OwnersById   = $ownersById
        CopyCount    = $copyCount
        SkippedFiles = $skippedFiles.ToArray()
        Replacement  = $merge.Replacement
    }
}
