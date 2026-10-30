# Reads the IDs one spreadsheet defines across the sources, as the game merges every source's copy of it.
# The game keeps the first row of each key in search order, and that row decides whether an ID is defined.
# Its owners are every source whose own row defines the same ID: removing one of them leaves the ID defined.
# A mod whose mod_info.json lists the spreadsheet's path under "replace" stands alone:
# the game reads only its copy, the first such mod's in search order, and drops the rows of every other.
# The game compares that path as written, slashes included.
# A copy that does not parse, or lacks a column the game reads, is skipped and returned with the reason.
function Read-SpreadsheetOwners {

    param(
        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources,

        # The spreadsheet, as New-SpreadsheetSource describes it.
        [object] $Spreadsheet
    )

    $replacingSource = Find-ReplacingSource -Sources $Sources -Path $Spreadsheet.Path

    $readSources = if ($replacingSource) {
        @($replacingSource)
    } else {
        $Sources
    }

    $ownersById = New-ContentOwnerIndex
    $skippedFiles = [System.Collections.Generic.List[object]]::new()
    $seenKeys = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $definedIds = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $readCopies = [System.Collections.Generic.List[object]]::new()
    $copyCount = 0

    foreach ($source in $readSources) {

        # GetFullPath turns the spreadsheet path's slashes into the system's, so a path reads as one.
        $path = [System.IO.Path]::GetFullPath($Spreadsheet.Path, $source.Folder)

        if (-not [System.IO.File]::Exists($path)) {
            continue
        }

        $copyCount++

        try {
            $text = Read-StarsectorText -Path $path

            if ($Spreadsheet.ConvertsCurlyQuotes) {
                $text = ConvertTo-StraightQuotes -Text $text
            }

            $rows = ConvertFrom-StarsectorCsv -Text $text -ColumnNames $Spreadsheet.ColumnNames
            $copyRows = ConvertTo-SpreadsheetRowIds -Rows $rows -Spreadsheet $Spreadsheet

        } catch {

            $skippedFiles.Add([pscustomobject]@{ Path = $path; Reason = $_.Exception.Message })
            continue
        }

        for ($index = 0; $index -lt $copyRows.Keys.Count; $index++) {

            if ($seenKeys.Add($copyRows.Keys[$index]) -and $copyRows.Ids[$index]) {
                $definedIds.Add($copyRows.Ids[$index]) | Out-Null
            }
        }

        $readCopies.Add([pscustomobject]@{ Owner = $source.Owner; Ids = $copyRows.Ids })
    }

    foreach ($copy in $readCopies) {
        foreach ($id in $copy.Ids) {

            if ($id -and $definedIds.Contains($id)) {
                Add-ContentOwner -OwnersById $ownersById -Id $id -Owner $copy.Owner
            }
        }
    }

    $replacingOwner = if ($replacingSource) {
        $replacingSource.Owner
    } else {
        $null
    }

    return [pscustomobject]@{
        OwnersById     = $ownersById
        CopyCount      = $copyCount
        SkippedFiles   = $skippedFiles.ToArray()
        ReplacingOwner = $replacingOwner
    }
}
