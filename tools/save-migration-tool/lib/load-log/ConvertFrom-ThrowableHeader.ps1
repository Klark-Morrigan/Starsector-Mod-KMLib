# Reads one stack trace line as a throwable's type and message, or null when the line names no throwable.
# A trace's own header and each "Caused by:" line name a throwable at column 0.
# Frames and "Suppressed:" blocks are indented, and log lines open with a timestamp, so neither matches.
function ConvertFrom-ThrowableHeader {

    param([string] $Line)

    $throwableHeaderPattern =
        '^(?:Caused by: )?(?<type>(?:[A-Za-z_$][\w$]*\.)+[\w$]*(?:Exception|Error|Throwable))(?:: (?<message>.*))?$'

    $match = [regex]::Match($Line, $throwableHeaderPattern)

    if (-not $match.Success) {
        return $null
    }

    return [pscustomobject]@{
        Type    = $match.Groups['type'].Value
        Message = $match.Groups['message'].Value
    }
}
