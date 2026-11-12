# A hash of where each file is, how long it is and when it was last written, for the cache key.
# Reading tens of thousands of files to hash their content would cost most of what the cache saves;
# an edited, added, removed or replaced file changes its stamp, or the list of paths.
function Get-FileStampHash {

    param(
        # Full paths, in the order the hash takes them.
        [string[]] $Paths
    )

    $stampLines = foreach ($path in $Paths) {

        $file = [System.IO.FileInfo]::new($path)

        # A file gone since it was listed reads as missing, so the next run's listing differs from this one.
        if ($file.Exists) {
            "$path|$($file.Length)|$($file.LastWriteTimeUtc.Ticks)"
        } else {
            "$path|missing"
        }
    }

    return Get-TextHash -Text ($stampLines -join "`n")
}
