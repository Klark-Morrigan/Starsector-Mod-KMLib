# Streams log files as one log and returns every save load attempt in order,
# with how many lines and attempts each file held.
# A failed attempt carries its root cause, and the object chain XStream's debugging information names.
# An attempt's lines may straddle a rotation, so the parse state carries across files.
# A relative save path resolves against SaveBaseDirectory, the game's working directory.
#   Depends on ConvertFrom-LogLine, ConvertFrom-ThrowableHeader, ConvertFrom-XStreamClassLine and
#   Get-LoadOutcomeNames being dot-sourced into the same scope.
function Read-LoadAttempt {

    param(
        [string[]] $LogFiles,
        [string] $SaveBaseDirectory
    )

    # Every load line comes from this logger.
    # Its name is checked with Contains first, which spares the regex on almost every line of a long log.
    $loaderLoggerName = 'com.fs.starfarer.campaign.save.CampaignGameManager'

    $attemptStartPattern = '^Loading (?<path>.+)\.\.\.$'
    $attemptSuccessMessage = 'Finished loading'
    $attemptFailureMessage = 'Error loading'

    # The loader logs its exception at this level, then prints the trace beneath it.
    $exceptionLogLevel = 'ERROR'

    # XStream opens each block of its debugging information with this line.
    # The ERROR line and the exception header beneath it print the same block, so only the first is read.
    $debugBlockStartLine = '---- Debugging information ----'

    $outcomes = Get-LoadOutcomeNames
    $attempts = [System.Collections.Generic.List[object]]::new()
    $fileReads = [System.Collections.Generic.List[object]]::new()
    $currentAttempt = $null
    $isReadingCause = $false
    $debugBlockCount = 0

    foreach ($logFile in $LogFiles) {

        $lineNumber = 0
        $attemptCountBefore = $attempts.Count

        foreach ($line in [System.IO.File]::ReadLines($logFile)) {

            $lineNumber++
            $loaderLine = if ($line.Contains($loaderLoggerName)) {
                ConvertFrom-LogLine -Line $line -LoggerName $loaderLoggerName
            }

            if ($isReadingCause) {
                if ($null -eq $loaderLine) {

                    $header = ConvertFrom-ThrowableHeader -Line $line

                    # Java prints the chain outermost first, so the last header is the root cause.
                    if ($null -ne $header) {

                        $currentAttempt.CauseType = $header.Type
                        $currentAttempt.Cause = $header.Message
                        continue
                    }

                    if ($line -eq $debugBlockStartLine) {

                        $debugBlockCount++
                        continue
                    }

                    $chainLink = if ($debugBlockCount -eq 1) { ConvertFrom-XStreamClassLine -Line $line }

                    if ($null -ne $chainLink) {
                        $currentAttempt.ObjectChain.Add($chainLink)
                    }
                    continue
                }

                if ($loaderLine.Level -eq $exceptionLogLevel) { continue }

                $isReadingCause = $false
                $currentAttempt = $null
            }

            if ($null -eq $loaderLine) {
                continue
            }

            $start = [regex]::Match($loaderLine.Message, $attemptStartPattern)

            if ($start.Success) {

                $currentAttempt = [pscustomobject]@{
                    SavePath  = [System.IO.Path]::GetFullPath($start.Groups['path'].Value, $SaveBaseDirectory)
                    Outcome   = $outcomes.Unfinished
                    CauseType = $null
                    Cause       = $null
                    ObjectChain = [System.Collections.Generic.List[object]]::new()
                    LogFile     = $logFile
                    LogLine     = $lineNumber
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
                $isReadingCause = $true
                $debugBlockCount = 0
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
