# Reads one log file into a scan of the logs, and returns how many lines and attempts the file held.
# An attempt opens on the loader's "Loading" line and ends on its "Finished loading" or "Error loading" line.
# A failure's trace is every other line up to the loader's next non-ERROR line, which may lie in a later file.
# A relative save path resolves against the scan's SaveBaseDirectory, the game's working directory.
#
# The file is read whole, and only the lines naming the loader are parsed one by one.
function Read-LoadLogFile {

    param(
        [string] $LogFile,

        # The state New-LoadLogScan started, carried from the files read before this one.
        [object] $Scan
    )

    # Every load line comes from this logger.
    $loaderLoggerName = 'com.fs.starfarer.campaign.save.CampaignGameManager'

    $attemptStartPattern = '^Loading (?<path>.+)\.\.\.$'
    $attemptSuccessMessage = 'Finished loading'
    $attemptFailureMessage = 'Error loading'

    # The loader logs its exception at this level, then prints the trace beneath it.
    $exceptionLogLevel = 'ERROR'

    $outcomes = Get-LoadOutcomeNames
    $lines = [System.IO.File]::ReadAllLines($LogFile)
    $attemptCountBefore = $Scan.Attempts.Count

    # A trace the previous file ended inside goes on from this file's first line.
    $traceFrom = 0

    foreach ($index in Find-MatchingLineIndex -Lines $lines -Text $loaderLoggerName) {

        # Null for a stack frame naming the loader's class, which a trace takes with its other lines.
        $loaderLine = ConvertFrom-LogLine -Line $lines[$index] -LoggerName $loaderLoggerName

        if ($null -eq $loaderLine) {
            continue
        }

        if ($null -ne $Scan.TraceLines) {

            Add-LineRange -Target $Scan.TraceLines -Lines $lines -From $traceFrom -To ($index - 1)
            $traceFrom = $index + 1

            if ($loaderLine.Level -eq $exceptionLogLevel) {
                continue
            }

            $Scan.TraceLines = $null
            $Scan.CurrentAttempt = $null
        }

        $start = [regex]::Match($loaderLine.Message, $attemptStartPattern)

        if ($start.Success) {

            $Scan.CurrentAttempt = [pscustomobject](
                [ordered]@{
                    SavePath = [System.IO.Path]::GetFullPath($start.Groups['path'].Value, $Scan.SaveBaseDirectory)
                    Outcome  = $outcomes.Unfinished
                } +
                (New-FailureReading) +
                [ordered]@{
                    LogFile = $LogFile
                    LogLine = $index + 1
                }
            )

            $Scan.Attempts.Add($Scan.CurrentAttempt)
            continue
        }

        if ($null -eq $Scan.CurrentAttempt) {
            continue
        }

        if ($loaderLine.Message -eq $attemptSuccessMessage) {

            $Scan.CurrentAttempt.Outcome = $outcomes.Loaded
            $Scan.CurrentAttempt = $null

        } elseif ($loaderLine.Message -eq $attemptFailureMessage) {

            $Scan.CurrentAttempt.Outcome = $outcomes.Failed
            $Scan.TraceLines = [System.Collections.Generic.List[string]]::new()
            $Scan.TraceLinesByAttempt.Add($Scan.CurrentAttempt, $Scan.TraceLines)
            $traceFrom = $index + 1
        }
    }

    if ($null -ne $Scan.TraceLines) {
        Add-LineRange -Target $Scan.TraceLines -Lines $lines -From $traceFrom -To ($lines.Length - 1)
    }

    return [pscustomobject]@{
        LogFile      = $LogFile
        LineCount    = $lines.Length
        AttemptCount = $Scan.Attempts.Count - $attemptCountBefore
    }
}
