# Lists every copy of every data file one kind's definition reads, as full paths, without reading what they define:
# what the cache key of a kind needs to tell whether its files changed.
# It finds the copies as the readers find them, so a file added to a folder the game lists is found too.
# A file set a spreadsheet lists reads that spreadsheet to know its paths, through the run's cache.
function Get-ContentKindInputFiles {

    param(
        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources,

        # One definition of Get-ContentDefinitions.
        [object] $Definition,

        # The run's parsed files, as New-ParsedFileCache holds them.
        [object] $Cache
    )

    $files = [System.Collections.Generic.List[object]]::new()

    foreach ($part in $Definition.Parts) {

        $tablePaths = @(foreach ($spreadsheet in $part.Spreadsheets) { $spreadsheet.Path }) +
            @(foreach ($keyedFile in $part.KeyedFiles) { $keyedFile.Path })

        foreach ($fileSet in $part.FileSets) {

            if (-not $fileSet.ListedBy) {

                $files.AddRange([object[]] @(Find-ContentFiles `
                    -Sources $Sources `
                    -FileSet $fileSet))

                continue
            }

            $tablePaths += $fileSet.ListedBy.Path

            $listing = Read-SpreadsheetOwners `
                -Sources $Sources `
                -Spreadsheet $fileSet.ListedBy `
                -Cache $Cache

            $files.AddRange([object[]] @(
                Find-ListedContentFiles `
                    -Sources $Sources `
                    -RelativePaths ([string[]] $listing.OwnersById.Keys)))
        }

        if ($tablePaths.Count -gt 0) {

            $files.AddRange([object[]] @(Find-ListedContentFiles `
                -Sources $Sources `
                -RelativePaths $tablePaths))
        }
    }

    $paths = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase)

    foreach ($file in $files) {
        foreach ($copy in $file.Copies) {

            $paths.
                Add($copy.Path) |
                Out-Null
        }
    }

    $sortedPaths = [string[]] @($paths)

    [System.Array]::Sort(
        $sortedPaths,
        [System.StringComparer]::OrdinalIgnoreCase)

    return $sortedPaths
}
