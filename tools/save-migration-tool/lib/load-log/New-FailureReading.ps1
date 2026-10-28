# Returns the readings a failure trace gives an attempt, and its load after it, empty until the trace is read.
# Every attempt and load carries them from the start, so a loaded attempt reads as having no cause, not as a gap.
# The ordered dictionary splices into the caller's own fields in place, keeping the JSON's field order.
function New-FailureReading {

    return [ordered]@{
        CauseType   = $null
        Cause       = $null
        Missing     = $null
        ObjectChain = @()
    }
}
