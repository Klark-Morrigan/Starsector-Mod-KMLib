# Strips # and // line comments that sit outside string literals.
# A regex cannot do this safely in either direction: matching anywhere
# destroys the "//" in a URL and the "#" in a hex colour, while matching
# only whole lines misses the comments that trail data on the same line
# (which is how most mod_info.json files annotate their fields).
function Remove-JsonComment {

    param([string] $Text)

    $builder = [System.Text.StringBuilder]::new($Text.Length)
    $isInString = $false
    $isEscaped = $false
    $index = 0

    while ($index -lt $Text.Length) {

        $char = $Text[$index]

        if ($isInString) {

            [void] $builder.Append($char)

            if ($isEscaped) {
                $isEscaped = $false
            } elseif ($char -eq '\') {
                $isEscaped = $true
            } elseif ($char -eq '"') {
                $isInString = $false
            }

            $index++
            continue
        }

        if ($char -eq '"') {

            $isInString = $true
            [void] $builder.Append($char)
            $index++
            continue
        }

        $isComment = ($char -eq '#') -or
            ($char -eq '/' -and ($index + 1) -lt $Text.Length -and $Text[$index + 1] -eq '/')

        if ($isComment) {

            while ($index -lt $Text.Length -and $Text[$index] -ne "`n") {
                $index++
            }

            continue
        }

        [void] $builder.Append($char)
        $index++
    }

    return $builder.ToString()
}
