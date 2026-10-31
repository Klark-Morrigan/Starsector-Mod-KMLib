# Strips the # comment from one line as the game's LoadingUtils does: a # outside a string runs to the line's end.
# The game flips its in-string flag on every double quote, escaped or not.
# A carriage return is dropped and clears both flags, as a line break does.
function Remove-JsonLineComment {

    param([string] $Line)

    $builder = [System.Text.StringBuilder]::new($Line.Length)
    $isInComment = $false
    $isInString = $false

    foreach ($char in $Line.ToCharArray()) {

        if ($char -eq '"') {
            $isInString = -not $isInString
        }

        if ($char -eq "`r") {

            $isInComment = $false
            $isInString = $false

        } elseif ($char -eq '#' -and -not $isInString) {
            $isInComment = $true

        } elseif (-not $isInComment) {
            [void] $builder.Append($char)
        }
    }

    return $builder.ToString()
}
