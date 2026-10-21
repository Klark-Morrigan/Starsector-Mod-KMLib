# Reads a failed load attempt's trace: the lines the loader printed under its ERROR line.
# Returns the root cause's type and message, and the object chain of XStream's debugging information.
function ConvertFrom-FailureTrace {

    param([string[]] $Lines)

    # XStream opens each block of its debugging information with this line.
    # The ERROR line and the exception header beneath it print the same block, so only the first is read.
    $debugBlockStartLine = '---- Debugging information ----'

    $causeType = $null
    $cause = $null
    $objectChain = [System.Collections.Generic.List[object]]::new()
    $debugBlockCount = 0

    foreach ($line in $Lines) {

        $header = ConvertFrom-ThrowableHeader -Line $line

        # Java prints the chain outermost first, so the last header is the root cause.
        if ($null -ne $header) {

            $causeType = $header.Type
            $cause = $header.Message
            continue
        }

        if ($line -eq $debugBlockStartLine) {

            $debugBlockCount++
            continue
        }

        $chainLink = if ($debugBlockCount -eq 1) { ConvertFrom-XStreamClassLine -Line $line }

        if ($null -ne $chainLink) {
            $objectChain.Add($chainLink)
        }
    }

    return [pscustomobject]@{
        CauseType   = $causeType
        Cause       = $cause
        ObjectChain = $objectChain.ToArray()
    }
}
