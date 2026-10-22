# Strips # comments the way the game's LoadingUtils does before org.json parses a file.
# Matching that exactly keeps a file the game rejects from parsing here.
# Neither // nor /* */ is a comment to the game, so both stay.
# The game drops every carriage return, keeping only the line feed.
#
# The game resets its string and comment state at each line break, so each line is read on its own,
# and only a line holding a # needs reading character by character.
function Remove-JsonComment {

    param([string] $Text)

    $lineFeed = "`n"
    $carriageReturn = "`r"

    if (-not $Text.Contains('#')) {
        return $Text.Replace($carriageReturn, '')
    }

    $lines = $Text.Split($lineFeed)

    for ($index = 0; $index -lt $lines.Count; $index++) {

        $lines[$index] = if ($lines[$index].Contains('#')) {
            Remove-JsonLineComment -Line $lines[$index]
        } else {
            $lines[$index].Replace($carriageReturn, '')
        }
    }

    return $lines -join $lineFeed
}
