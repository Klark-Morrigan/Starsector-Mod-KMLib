# Strips # comments the way the game's LoadingUtils does before org.json parses a file.
# A # outside a string starts a comment that runs to the end of its line.
# The game flips its in-string flag on every double quote, escaped or not, and clears both flags at each line break.
# Matching that exactly keeps a file the game rejects from parsing here.
# Neither // nor /* */ is a comment to the game, so both stay.
# The game also drops every carriage return, keeping only the line feed.
function Remove-JsonComment {

    param([string] $Text)

    $builder = [System.Text.StringBuilder]::new($Text.Length)
    $isInComment = $false
    $isInString = $false

    foreach ($char in $Text.ToCharArray()) {

        if ($char -eq '"') {
            $isInString = -not $isInString
        }

        if ($char -eq "`n" -or $char -eq "`r") {

            $isInComment = $false
            $isInString = $false

            if ($char -eq "`n") {
                [void] $builder.Append($char)
            }

        } elseif ($char -eq '#' -and -not $isInString) {
            $isInComment = $true

        } elseif (-not $isInComment) {
            [void] $builder.Append($char)
        }
    }

    return $builder.ToString()
}
