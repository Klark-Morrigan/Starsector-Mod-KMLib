# Finds the index of every line holding the given text, case-sensitively, in order.
# The match runs as one array operation, several times faster than a loop over a log of hundreds of thousands of lines.
function Find-MatchingLineIndex {

    param(
        [string[]] $Lines,
        [string] $Text
    )

    $index = -1

    foreach ($matchingLine in @($Lines -cmatch [regex]::Escape($Text))) {

        # Searching on from the last hit finds a repeated line at each of its places in turn.
        $index = [System.Array]::IndexOf($Lines, $matchingLine, $index + 1)
        $index
    }
}
