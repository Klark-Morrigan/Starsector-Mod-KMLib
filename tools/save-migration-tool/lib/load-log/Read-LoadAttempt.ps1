# Reads log files as one log and returns every save load attempt in order,
# with how many lines and attempts each file held.
# A failed attempt carries its root cause, and the object chain XStream's debugging information names.
# An attempt's lines may straddle a rotation, so the parse state carries across files.
# A relative save path resolves against SaveBaseDirectory, the game's working directory.
#
# Each file is read whole, and only the lines naming the loader are parsed one by one.
# A failure's trace is every other line up to the loader's next non-ERROR line.
function Read-LoadAttempt {

    param(
        [string[]] $LogFiles,
        [string] $SaveBaseDirectory
    )

    # Every load line comes from this logger.
    $loaderLoggerName = 'com.fs.starfarer.campaign.save.CampaignGameManager'

    $attemptStartPattern = '^Loading (?<path>.+)\.\.\.$'
    $attemptSuccessMessage = 'Finished loading'
    $attemptFailureMessage = 'Error loading'

    # The loader logs its exception at this level, then prints the trace beneath it.
    $exceptionLogLevel = 'ERROR'

    $outcomes = Get-LoadOutcomeNames
    $attempts = [System.Collections.Generic.List[object]]::new()
    $fileReads = [System.Collections.Generic.List[object]]::new()
    $traces = [System.Collections.Generic.List[object]]::new()
    $currentAttempt = $null

    # The trace being read, or null outside one.
    $traceLines = $null

    foreach ($logFile in $LogFiles) {

        $lines = [System.IO.File]::ReadAllLines($logFile)
        $attemptCountBefore = $attempts.Count

        # A trace the previous file ended inside goes on from this file's first line.
        $traceFrom = 0

        foreach ($index in Find-MatchingLineIndex -Lines $lines -Text $loaderLoggerName) {

            # Null for a stack frame naming the loader's class, which a trace takes with its other lines.
            $loaderLine = ConvertFrom-LogLine -Line $lines[$index] -LoggerName $loaderLoggerName

            if ($null -eq $loaderLine) {
                continue
            }

            if ($null -ne $traceLines) {

                Add-LineRange -Target $traceLines -Lines $lines -From $traceFrom -To ($index - 1)
                $traceFrom = $index + 1

                if ($loaderLine.Level -eq $exceptionLogLevel) {
                    continue
                }

                $traceLines = $null
                $currentAttempt = $null
            }

            $start = [regex]::Match($loaderLine.Message, $attemptStartPattern)

            if ($start.Success) {

                $currentAttempt = [pscustomobject]@{
                    SavePath    = [System.IO.Path]::GetFullPath($start.Groups['path'].Value, $SaveBaseDirectory)
                    Outcome     = $outcomes.Unfinished
                    CauseType   = $null
                    Cause       = $null
                    ObjectChain = @()
                    LogFile     = $logFile
                    LogLine     = $index + 1
                }

                $attempts.Add($currentAttempt)
                continue
            }

            if ($null -eq $currentAttempt) {
                continue
            }

            if ($loaderLine.Message -eq $attemptSuccessMessage) {

                $currentAttempt.Outcome = $outcomes.Loaded
                $currentAttempt = $null

            } elseif ($loaderLine.Message -eq $attemptFailureMessage) {

                $currentAttempt.Outcome = $outcomes.Failed
                $traceLines = [System.Collections.Generic.List[string]]::new()
                $traceFrom = $index + 1
                $traces.Add([pscustomobject]@{ Attempt = $currentAttempt; Lines = $traceLines })
            }
        }

        if ($null -ne $traceLines) {
            Add-LineRange -Target $traceLines -Lines $lines -From $traceFrom -To ($lines.Length - 1)
        }

        $fileReads.Add([pscustomobject]@{
            LogFile      = $logFile
            LineCount    = $lines.Length
            AttemptCount = $attempts.Count - $attemptCountBefore
        })
    }

    foreach ($trace in $traces) {

        $failure = ConvertFrom-FailureTrace -Lines $trace.Lines.ToArray()

        $trace.Attempt.CauseType = $failure.CauseType
        $trace.Attempt.Cause = $failure.Cause
        $trace.Attempt.ObjectChain = $failure.ObjectChain
    }

    return [pscustomobject]@{
        Attempts  = $attempts.ToArray()
        FileReads = $fileReads.ToArray()
    }
}
