# Starts the state a scan of the logs carries from one file to the next: an attempt's lines may straddle a rotation.
# Every attempt read so far, the trace lines of each failed one, and the attempt and trace still open.
# A trace is kept apart from its attempt and read only for the loads a caller keeps, since reading one costs time.
function New-LoadLogScan {

    param([string] $SaveBaseDirectory)

    # Attempts are objects without a value identity, so their traces are filed by reference.
    # Each value is the attempt's trace lines, a List[string] Read-LoadLogFile appends to.
    $byReference = [System.Collections.Generic.ReferenceEqualityComparer]::Instance
    $traceLinesByAttempt = [System.Collections.Generic.Dictionary[object, object]]::new($byReference)

    return [pscustomobject]@{
        SaveBaseDirectory   = $SaveBaseDirectory
        Attempts            = [System.Collections.Generic.List[object]]::new()
        TraceLinesByAttempt = $traceLinesByAttempt
        CurrentAttempt      = $null

        # The trace being read, or null outside one.
        TraceLines          = $null
    }
}
