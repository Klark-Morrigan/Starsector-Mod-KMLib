# Folds vanilla's retries into the load that caused them.
# An attempt is a retry when the one before it failed on the same save and the load has not yet used every source.
# A player loading the same broken save twice so still reads as two loads.
#   Depends on Get-LoadOutcomeNames being dot-sourced into the same scope.
function Group-LoadAttempt {

    param([object[]] $Attempts)

    # The files a load reads, in the order vanilla tries them.
    # The load method calls itself again on failure, first with its bak flag, then with its inprogress flag.
    # campaign stands for campaign.xml, or campaign.zip for a compressed save.
    $attemptSources = @('campaign', 'campaign.bak', 'campaign.inprogress')

    $failedOutcome = (Get-LoadOutcomeNames).Failed
    $loads = [System.Collections.Generic.List[object]]::new()
    $currentAttempts = $null

    foreach ($attempt in $Attempts) {

        $previous = if ($null -ne $currentAttempts) { $currentAttempts[$currentAttempts.Count - 1] } else { $null }
        $isRetry = $null -ne $previous -and
            $previous.Outcome -eq $failedOutcome -and
            $previous.SavePath -eq $attempt.SavePath -and
            $currentAttempts.Count -lt $attemptSources.Count

        if (-not $isRetry) {

            $currentAttempts = [System.Collections.Generic.List[object]]::new()
            $loads.Add($currentAttempts)
        }

        $currentAttempts.Add($attempt)
    }

    foreach ($loadAttempts in $loads) {

        $firstAttempt = $loadAttempts[0]
        $sourcedAttempts = for ($index = 0; $index -lt $loadAttempts.Count; $index++) {

            $attempt = $loadAttempts[$index]

            [pscustomobject]@{
                Source      = $attemptSources[$index]
                Outcome     = $attempt.Outcome
                CauseType   = $attempt.CauseType
                Cause       = $attempt.Cause
                ObjectChain = @($attempt.ObjectChain)
                LogFile     = $attempt.LogFile
                LogLine     = $attempt.LogLine
            }
        }

        [pscustomobject]@{
            SaveFolder = Split-Path -Leaf $firstAttempt.SavePath
            SavePath   = $firstAttempt.SavePath
            Outcome    = $loadAttempts[$loadAttempts.Count - 1].Outcome
            CauseType  = $firstAttempt.CauseType
            Cause      = $firstAttempt.Cause
            Attempts   = @($sourcedAttempts)
        }
    }
}
