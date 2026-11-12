# Says whether JSON text holds a // or /* comment outside its strings.
# The game's org.json rejects both, while ConvertFrom-Json skips them, so a reader has to look for them itself.
# Strings are matched first and kept whole, as org.json reads them, so a URL inside a string is not a comment.
function Test-JsonSlashComment {

    param([string] $Text)

    # Most files hold no slash pair at all, and the match below walks every string in the file.
    if (-not ($Text.Contains('//') -or $Text.Contains('/*'))) {
        return $false
    }

    $stringPattern = Get-JsonStringPattern

    foreach ($token in [regex]::Matches(
            $Text,
            "$stringPattern|(?<comment>/[/*])")) {

        if ($token.Groups['comment'].Success) {
            return $true
        }
    }

    return $false
}
