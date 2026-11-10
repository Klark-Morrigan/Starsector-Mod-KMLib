# Strips # comments the way the game's LoadingUtils does before org.json parses a file.
# Matching that exactly keeps a file the game rejects from parsing here.
# Neither // nor /* */ is a comment to the game, so both stay.
# The game drops every carriage return as it reads a file, before the strip runs,
# so a carriage return ends no string and no comment.
#
# The game's rule holds within a line: a # outside a string runs to the line's end,
# and a string runs from a quote to the next quote or the line's end.
# A backslash escapes nothing, since the game flips its in-string flag on every quote.
# One regex replace reads the whole text, because a loop over its characters in PowerShell is many times slower.
function Remove-JsonComment {

    param([string] $Text)

    $textWithoutReturns = $Text.Replace("`r", '')

    if (-not $textWithoutReturns.Contains('#')) {
        return $textWithoutReturns
    }

    $stringPattern = '"[^"\n]*"?'
    $outsideStringPattern = '[^"#\n]+'

    # The kept part is atomic: backing off the end of an unclosed string would take a # inside it for a comment,
    # where the game keeps it.
    $keptPattern = "(?<kept>(?>(?:$outsideStringPattern|$stringPattern)*))"

    return [regex]::Replace(
        $textWithoutReturns,
        "(?m)^$keptPattern#[^\n]*",
        '${kept}')
}
