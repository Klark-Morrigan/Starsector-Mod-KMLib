# Streams log files as one log and returns every save load attempt in order,
# with how many lines and attempts each file held.
# An attempt's lines may straddle a rotation, so the parse state carries across files.
# A relative save path resolves against SaveBaseDirectory, the game's working directory.
#   Depends on Get-LoadOutcomeNames being dot-sourced into the same scope.
function Read-LoadAttempt {

    param(
        [string[]] $LogFiles,
        [string] $SaveBaseDirectory
    )

    # Every load line comes from this logger; its name gates the cheap pre-check on each log line.
    $loaderLoggerName = 'com.fs.starfarer.campaign.save.CampaignGameManager'

    # A log line from the loader: "<ms> [<thread>] <LEVEL> <logger>  - <message>".
    # Anchoring on the timestamp matters: the loader's own stack frames name the same class.
    $loaderLogLinePattern = "^\d+ \[[^\]]*\] (?<level>\w+)\s+$([regex]::Escape($loaderLoggerName))\s+- (?<message>.*)$"

    $attemptStartPattern = '^Loading (?<path>.+)\.\.\.$'
    $attemptSuccessMessage = 'Finished loading'
    $attemptFailureMessage = 'Error loading'

    # The loader logs its exception at this level, then prints the trace beneath it.
    $exceptionLogLevel = 'ERROR'

    # A trace's own header and each "Caused by:" line name a throwable at column 0.
    # Frames and "Suppressed:" blocks are indented, and log lines open with a timestamp, so neither matches.
    $throwableHeaderPattern =
        '^(?:Caused by: )?(?<type>(?:[A-Za-z_$][\w$]*\.)+[\w$]*(?:Exception|Error|Throwable))(?:: (?<message>.*))?$'

    $outcomes = Get-LoadOutcomeNames
    $attempts = [System.Collections.Generic.List[object]]::new()
    $fileReads = [System.Collections.Generic.List[object]]::new()
    $currentAttempt = $null
    $isReadingCause = $false

    foreach ($logFile in $LogFiles) {

        $lineNumber = 0
        $attemptCountBefore = $attempts.Count

        foreach ($line in [System.IO.File]::ReadLines($logFile)) {

            $lineNumber++
            $loaderLine = if ($line.Contains($loaderLoggerName)) { [regex]::Match($line, $loaderLogLinePattern) }
            $isLoaderLine = $null -ne $loaderLine -and $loaderLine.Success

            if ($isReadingCause) {
                if (-not $isLoaderLine) {

                    $header = [regex]::Match($line, $throwableHeaderPattern)

                    # Java prints the chain outermost first, so the last header is the root cause.
                    if ($header.Success) {
                        $currentAttempt.CauseType = $header.Groups['type'].Value
                        $currentAttempt.Cause = $header.Groups['message'].Value
                    }
                    continue
                }

                if ($loaderLine.Groups['level'].Value -eq $exceptionLogLevel) { continue }

                $isReadingCause = $false
                $currentAttempt = $null
            }

            if (-not $isLoaderLine) {
                continue
            }

            $message = $loaderLine.Groups['message'].Value
            $start = [regex]::Match($message, $attemptStartPattern)

            if ($start.Success) {

                $currentAttempt = [pscustomobject]@{
                    SavePath  = [System.IO.Path]::GetFullPath($start.Groups['path'].Value, $SaveBaseDirectory)
                    Outcome   = $outcomes.Unfinished
                    CauseType = $null
                    Cause     = $null
                    LogFile   = $logFile
                    LogLine   = $lineNumber
                }

                $attempts.Add($currentAttempt)
                continue
            }

            if ($null -eq $currentAttempt) {
                continue
            }

            if ($message -eq $attemptSuccessMessage) {

                $currentAttempt.Outcome = $outcomes.Loaded
                $currentAttempt = $null

            } elseif ($message -eq $attemptFailureMessage) {

                $currentAttempt.Outcome = $outcomes.Failed
                $isReadingCause = $true
            }
        }

        $fileReads.Add([pscustomobject]@{
            LogFile      = $logFile
            LineCount    = $lineNumber
            AttemptCount = $attempts.Count - $attemptCountBefore
        })
    }

    return [pscustomobject]@{
        Attempts  = $attempts.ToArray()
        FileReads = $fileReads.ToArray()
    }
}
