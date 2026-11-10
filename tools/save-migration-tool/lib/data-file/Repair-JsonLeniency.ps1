# Rewrites what org.json takes and JSON refuses, so ConvertFrom-Json reads a file the game reads.
# Each repair follows the json.jar the game ships:
# - It reads one root value and ignores whatever follows it, so text after the root is dropped.
# - It takes ; between entries as a comma, and = or => between a key and its value as a colon.
# - It reads an unquoted value up to the next , : ] } / \ " [ { ; = or #, trims it, and reads it as a value:
#   True as a boolean, 049 as 49, 0.5f as 0.5, and a bare word as text.
#   Each is written as the JSON of that value, a value JSON already takes too, so one rule reads them all.
# Quoted strings are matched first and kept whole, so nothing inside one is taken for structure.
function Repair-JsonLeniency {

    param([string] $Text)

    $stringPattern = '"(?:[^"\\]|\\.)*"|''(?:[^''\\]|\\.)*'''

    # The delimiter after a value is looked at, not taken, so it can open the next value.
    $bareValuePattern = '(?<lead>[:\[,]\s*)(?<value>[^\s"'',:\]}/\\\[{;=#][^",:\]}/\\\[{;=#\r\n]*?)(?=\s*[,\]}])'
    $separatorPattern = '(?<entrySeparator>;)|(?<keySeparator>=>?)'

    $rootEnd = -1
    $depth = 0

    foreach ($token in [regex]::Matches(
            $Text,
            "$stringPattern|[{}\[\]]")) {

        switch ($token.Value) {

            { $_ -in '{', '[' } {
                $depth++
            }

            { $_ -in '}', ']' } {

                $depth--

                if ($depth -eq 0 `
                        -and $rootEnd -lt 0) {

                    $rootEnd = $token.Index
                }
            }
        }
    }

    $rootText = if ($rootEnd -ge 0) {
        $Text.Substring(0, $rootEnd + 1)
    } else {
        $Text
    }

    $repairToken = {

        param($match)

        if ($match.Groups['entrySeparator'].Success) {
            return ','
        }

        if ($match.Groups['keySeparator'].Success) {
            return ':'
        }

        if (-not $match.Groups['value'].Success) {
            return $match.Value
        }

        return $match.Groups['lead'].Value
            + (ConvertTo-JsonValueText -Text $match.Groups['value'].Value)
    }

    # Separators are rewritten first, so a value after = or ; has the : or , its pattern looks for.
    $separatedText = [regex]::Replace(
        $rootText,
        "$stringPattern|$separatorPattern",
        $repairToken)

    return [regex]::Replace(
        $separatedText,
        "$stringPattern|$bareValuePattern",
        $repairToken)
}
