# Reads the IDs one kind's sources define, each with its owners, as one definition of Get-ContentDefinitions
# lays them out: an ID one part defines is defined.
# Within a part, content files and spreadsheets together need both, as New-ContentPart describes;
# keyed JSON files and the game's built-in IDs add theirs either way.
# Files and spreadsheets that do not parse are skipped and returned with the reason,
# and each file or spreadsheet a mod replaces is returned with that mod.
# Returns how many content file copies it read; spreadsheets and keyed JSON files are parsed through the run's cache,
# which counts them once however many kinds read them.
function Read-KindContentOwners {

    param(
        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources,

        # One definition of Get-ContentDefinitions.
        [object] $Definition,

        # The run's parsed files, as New-ParsedFileCache holds them.
        [object] $Cache
    )

    $coreOwner = Get-CoreOwnerName
    $kindOwners = New-ContentOwnerIndex
    $skippedFiles = [System.Collections.Generic.List[object]]::new()
    $replacements = [System.Collections.Generic.List[object]]::new()
    $contentFileCount = 0

    foreach ($part in $Definition.Parts) {

        $fileOwners = New-ContentOwnerIndex
        $rowOwners = New-ContentOwnerIndex

        foreach ($fileSet in $part.FileSets) {

            $read = Read-ContentFileOwners -Sources $Sources -FileSet $fileSet -Cache $Cache

            $contentFileCount += $read.CopyCount
            $skippedFiles.AddRange([object[]] $read.SkippedFiles)
            $replacements.AddRange([object[]] $read.Replacements)
            $fileOwners = Merge-ContentOwners -Into $fileOwners -From $read.OwnersById
        }

        foreach ($spreadsheet in $part.Spreadsheets) {

            $read = Read-SpreadsheetOwners -Sources $Sources -Spreadsheet $spreadsheet -Cache $Cache

            $skippedFiles.AddRange([object[]] $read.SkippedFiles)

            if ($read.Replacement) {
                $replacements.Add($read.Replacement)
            }

            $rowOwners = Merge-ContentOwners -Into $rowOwners -From $read.OwnersById
        }

        $partOwners = if ($part.FileSets.Count -gt 0 -and $part.Spreadsheets.Count -gt 0) {
            Select-ContentWithRow -FileOwners $fileOwners -RowOwners $rowOwners
        } else {
            Merge-ContentOwners -Into $fileOwners -From $rowOwners
        }

        foreach ($keyedFile in $part.KeyedFiles) {

            $read = Read-KeyedFileOwners -Sources $Sources -KeyedFile $keyedFile -Cache $Cache

            $skippedFiles.AddRange([object[]] $read.SkippedFiles)

            if ($read.Replacement) {
                $replacements.Add($read.Replacement)
            }

            $partOwners = Merge-ContentOwners -Into $partOwners -From $read.OwnersById
        }

        foreach ($builtInId in $part.BuiltInIds) {
            Add-ContentOwner -OwnersById $partOwners -Id $builtInId -Owner $coreOwner
        }

        $kindOwners = Merge-ContentOwners -Into $kindOwners -From $partOwners
    }

    return [pscustomobject]@{
        OwnersById       = $kindOwners
        ContentFileCount = $contentFileCount
        SkippedFiles     = $skippedFiles.ToArray()
        Replacements     = $replacements.ToArray()
    }
}
