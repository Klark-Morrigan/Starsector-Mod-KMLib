# Reads log files as one log and returns every save load attempt in order,
# with how many lines and attempts each file held.
# A failed attempt's trace comes back apart from it, in TraceLinesByAttempt, for Add-FailureReading to read later.
function Read-LoadAttempt {

    param(
        [string[]] $LogFiles,
        [string] $SaveBaseDirectory
    )

    $scan = New-LoadLogScan -SaveBaseDirectory $SaveBaseDirectory
    $fileReads = @(foreach ($logFile in $LogFiles) { Read-LoadLogFile -LogFile $logFile -Scan $scan })

    return [pscustomobject]@{
        Attempts            = $scan.Attempts.ToArray()
        TraceLinesByAttempt = $scan.TraceLinesByAttempt
        FileReads           = $fileReads
    }
}
