# Reads the trace of each failed attempt of a load: the root cause, the object chain, and the missing content
# the cause names. An attempt with no trace keeps its empty readings.
function Add-FailureReading {

    param(
        [object] $Load,

        # Each failed attempt's trace lines, as Read-LoadAttempt files them.
        [System.Collections.IDictionary] $TraceLinesByAttempt
    )

    foreach ($attempt in $Load.Attempts) {

        if (-not $TraceLinesByAttempt.ContainsKey($attempt)) {
            continue
        }

        $failure = ConvertFrom-FailureTrace -Lines $TraceLinesByAttempt[$attempt].ToArray()

        $attempt.CauseType = $failure.CauseType
        $attempt.Cause = $failure.Cause
        $attempt.ObjectChain = $failure.ObjectChain
        $attempt.Missing = Get-MissingContent -CauseType $failure.CauseType -Cause $failure.Cause
    }

    return $Load
}
