# Says whether JSON text holds a // or /* comment outside its strings.
# The game's org.json rejects both, while ConvertFrom-Json skips them, so a reader has to look for them itself.
# Strings open on either quote, as org.json's do, so a URL inside a string is not a comment.
function Test-JsonSlashComment {

    param([string] $Text)

    # Most files hold no slash pair at all, and the scan below runs per character.
    if (-not ($Text.Contains('//') -or $Text.Contains('/*'))) {
        return $false
    }

    $openQuote = $null
    $isEscaped = $false

    for ($index = 0; $index -lt $Text.Length; $index++) {

        $char = $Text[$index]

        if ($null -ne $openQuote) {

            if ($isEscaped) {
                $isEscaped = $false

            } elseif ($char -eq '\') {
                $isEscaped = $true

            } elseif ($char -eq $openQuote) {
                $openQuote = $null
            }

        } elseif ($char -eq '"' -or $char -eq "'") {
            $openQuote = $char

        } elseif ($char -eq '/' -and $index + 1 -lt $Text.Length -and $Text[$index + 1] -in '/', '*') {
            return $true
        }
    }

    return $false
}
