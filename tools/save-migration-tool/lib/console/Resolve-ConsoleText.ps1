# Returns painted text as a stream can take it: as it is on a console, and with its colour stripped otherwise.
# A file, a pipe into another program or a caller parsing the output would read colour codes as data.
# NO_COLOR, set to anything, turns colour off on a console too, as https://no-color.org asks.
function Resolve-ConsoleText {

    param(
        [string] $Text,

        [ValidateSet('Output', 'Error')]
        [string] $Stream
    )

    if ($env:NO_COLOR -or (Test-ConsoleStreamRedirected -Stream $Stream)) {
        return $Text -replace "`e\[[0-9;]*m", ''
    }

    return $Text
}
