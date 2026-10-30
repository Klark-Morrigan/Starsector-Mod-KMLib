# Writes the diagnostic lines for the copies the catalogue skipped, one line per reason, reasons in the order found.
# One broken rule can skip every copy of a kind, and a line per copy would bury the rest of the log.
# A reason reads as the problem, then ": " and what is particular to the copy, such as its path,
# so copies are grouped by the text before the first ": ". A reason only one copy gives is written whole.
function Format-SkippedFileLines {

    param(
        # The skipped copies, each with its path and its reason, as New-ContentCatalogue returns them.
        [object[]] $SkippedFiles
    )

    # Enough paths to find the copies by, few enough to keep the line short.
    $shownPathCount = 3
    $reasonDetailSeparator = ': '

    $filesByReason = [System.Collections.Specialized.OrderedDictionary]::new([System.StringComparer]::Ordinal)

    foreach ($skippedFile in $SkippedFiles) {

        $separatorIndex = $skippedFile.Reason.IndexOf($reasonDetailSeparator, [System.StringComparison]::Ordinal)

        $reason = if ($separatorIndex -lt 0) {
            $skippedFile.Reason
        } else {
            $skippedFile.Reason.Substring(0, $separatorIndex)
        }

        if (-not $filesByReason.Contains($reason)) {
            $filesByReason[$reason] = [System.Collections.Generic.List[object]]::new()
        }

        $filesByReason[$reason].Add($skippedFile)
    }

    $lines = foreach ($reason in $filesByReason.Keys) {

        $files = $filesByReason[$reason]

        if ($files.Count -eq 1) {

            "skipped $(Format-ConsoleText -Role 'Path' -Text $files[0].Path): $($files[0].Reason)"
            continue
        }

        $shownFiles = $files | Select-Object -First $shownPathCount
        $shownPaths = Format-ConsoleList -Role 'Path' -Values ([string[]] $shownFiles.Path)
        $hiddenCount = $files.Count - $shownPathCount

        $hiddenNote = if ($hiddenCount -gt 0) {
            " and $(Format-ConsoleNumber $hiddenCount) more"
        } else {
            ''
        }

        "skipped $(Format-ConsoleNumber $files.Count) files ($reason): $shownPaths$hiddenNote"
    }

    return $lines
}
