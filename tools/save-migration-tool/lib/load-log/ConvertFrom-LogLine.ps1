# Reads one log4j line from the given logger as its level and message, or null for any other line.
# The line must open with a timestamp: a stack frame names the logger's class too, and is not a log line.
function ConvertFrom-LogLine {

    param(
        [string] $Line,
        [string] $LoggerName
    )

    # "<ms> [<thread>] <LEVEL> <logger>  - <message>".
    $logLinePattern = "^\d+ \[[^\]]*\] (?<level>\w+)\s+$([regex]::Escape($LoggerName))\s+- (?<message>.*)$"

    $match = [regex]::Match($Line, $logLinePattern)

    if (-not $match.Success) {
        return $null
    }

    return [pscustomobject]@{
        Level   = $match.Groups['level'].Value
        Message = $match.Groups['message'].Value
    }
}
