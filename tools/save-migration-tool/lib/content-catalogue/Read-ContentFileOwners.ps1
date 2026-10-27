# Reads the ID each content file of a file set defines, and the sources whose copies set it.
# The game merges the copies of a file, so the last copy in merge order that sets the ID decides it,
# and a copy that sets no ID only patches the others.
# Every copy that sets the ID the merge ends with owns it: mods often restate the game's own file in full,
# and removing one of them leaves the ID defined.
# A copy that does not parse is skipped and returned with the reason:
# one broken copy must not hide the content of every other. An empty ID defines nothing.
# A set listed by a spreadsheet reads that spreadsheet first, and counts it.
function Read-ContentFileOwners {

    param(
        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources,

        # One file set, as New-ContentFileSet describes it.
        [object] $FileSet
    )

    $ownersById = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]]::new()
    $skippedFiles = [System.Collections.Generic.List[object]]::new()
    $copyCount = 0
    $spreadsheetCount = 0

    if ($FileSet.ListedBy) {

        $listing = Read-SpreadsheetOwners -Sources $Sources -Spreadsheet $FileSet.ListedBy

        $spreadsheetCount = $listing.FileCount
        $skippedFiles.AddRange([object[]] $listing.SkippedFiles)
        $files = @(Find-ListedContentFiles -Sources $Sources -RelativePaths ([string[]] $listing.OwnersById.Keys))

    } else {
        $files = @(Find-ContentFiles -Sources $Sources -FileSet $FileSet)
    }

    foreach ($file in $files) {

        # Most files have one copy, which needs no ordering.
        $mergedCopies = if ($file.Copies.Count -eq 1) {
            $file.Copies
        } else {
            Select-MergedCopies -File $file
        }

        $id = $null
        $idSetters = [System.Collections.Generic.List[object]]::new()

        foreach ($copy in $mergedCopies) {

            $copyCount++

            try {
                $copyId = Read-ContentFileId -Path $copy.Path -IdKey $FileSet.IdKey

            } catch {

                $skippedFiles.Add([pscustomobject]@{ Path = $copy.Path; Reason = $_.Exception.Message })
                continue
            }

            if ($null -ne $copyId) {

                $id = $copyId
                $idSetters.Add([pscustomobject]@{ Owner = $copy.Source.Owner; Id = $copyId })
            }
        }

        if (-not $id) {
            continue
        }

        foreach ($idSetter in $idSetters) {

            if ($idSetter.Id -ceq $id) {
                Add-ContentOwner -OwnersById $ownersById -Id $id -Owner $idSetter.Owner
            }
        }
    }

    return [pscustomobject]@{
        OwnersById       = $ownersById
        FileCount        = $copyCount
        SpreadsheetCount = $spreadsheetCount
        SkippedFiles     = $skippedFiles.ToArray()
    }
}
