# Reads the IDs one spreadsheet defines across the sources, as the game merges every source's copy of it.
# The game keeps the first row of each key in search order, dropping a row whose key columns are all empty,
# and only then applies the source's row filter and reads the row's ID. That row decides whether an ID is defined.
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

    $hashMark = '#'

    # The key columns, then the ID and filter columns where those differ, each asked of the reader once.
    $columnNames = [System.Collections.Generic.List[string]]::new([string[]] $Spreadsheet.KeyColumns)

    foreach ($extraColumn in @($Spreadsheet.IdColumn, $Spreadsheet.RowFilterColumn)) {

        if ($extraColumn -and -not $columnNames.Contains($extraColumn)) {
            $columnNames.Add($extraColumn)
        }
    }

    $keyCount = $Spreadsheet.KeyColumns.Count

    $idIndex = if ($Spreadsheet.IdColumn) {
        $columnNames.IndexOf($Spreadsheet.IdColumn)
    } else {
        -1
    }

    $filterIndex = if ($Spreadsheet.RowFilterColumn) {
        $columnNames.IndexOf($Spreadsheet.RowFilterColumn)
    } else {
        -1
    }

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
    $fileCount = 0

    foreach ($source in $readSources) {

        # GetFullPath turns the spreadsheet path's slashes into the system's, so a path reads as one.
        $path = [System.IO.Path]::GetFullPath($Spreadsheet.Path, $source.Folder)

        if (-not [System.IO.File]::Exists($path)) {
            continue
        }

        $fileCount++

        # Each row the copy keeps: its merge key, and its ID, or null when it defines none, at the same index.
        $copyKeys = [System.Collections.Generic.List[string]]::new()
        $copyIds = [System.Collections.Generic.List[string]]::new()

        try {
            $text = Read-StarsectorText -Path $path

            if ($Spreadsheet.ConvertsCurlyQuotes) {
                $text = ConvertTo-StraightQuotes -Text $text
            }

            # The reader returns its rows as one array, which a foreach over the call would take as one row.
            $rows = ConvertFrom-StarsectorCsv -Text $text -ColumnNames $columnNames.ToArray()

            foreach ($row in $rows) {

                $keyParts = $row[0..($keyCount - 1)]

                # The game fails on a row lacking a column it reads.
                for ($column = 0; $column -lt $columnNames.Count; $column++) {

                    if ($null -eq $row[$column] -and $column -ne $filterIndex) {
                        throw "has no $($columnNames[$column]) column in every row"
                    }
                }

                if (-not ($keyParts -join '')) {
                    continue
                }

                $isFiltered = $filterIndex -ge 0 -and $row[$filterIndex] -cne $Spreadsheet.RowFilterValue
                $isPartialKey = $Spreadsheet.RequiresEveryKeyColumn -and
                    @($keyParts | Where-Object { -not $_ }).Count -gt 0

                $id = if ($idIndex -ge 0) {
                    $row[$idIndex]
                } else {
                    $keyParts -join $Spreadsheet.IdJoiner
                }

                $isDefining = -not $isFiltered -and -not $isPartialKey -and $id -and
                    -not ($Spreadsheet.SkipsHashIds -and $id.StartsWith($hashMark))

                $definedId = if ($isDefining) {
                    $id
                } else {
                    $null
                }

                $copyKeys.Add($keyParts -join "`0")
                $copyIds.Add($definedId)
            }

        } catch {

            $skippedFiles.Add([pscustomobject]@{ Path = $path; Reason = $_.Exception.Message })
            continue
        }

        for ($index = 0; $index -lt $copyKeys.Count; $index++) {

            if ($seenKeys.Add($copyKeys[$index]) -and $copyIds[$index]) {
                $definedIds.Add($copyIds[$index]) | Out-Null
            }
        }

        $readCopies.Add([pscustomobject]@{ Owner = $source.Owner; Ids = $copyIds })
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
        FileCount      = $fileCount
        SkippedFiles   = $skippedFiles.ToArray()
        ReplacingOwner = $replacingOwner
    }
}
