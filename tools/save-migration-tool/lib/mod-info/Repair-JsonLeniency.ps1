# Rewrites what org.json takes and JSON refuses, so ConvertFrom-Json reads a file the game reads.
# Each repair follows the json.jar the game ships:
# - It reads one root value and ignores whatever follows it, so text after the root is dropped.
# - It takes ; between entries as a comma, and = or => between a key and its value as a colon.
# - It reads an unquoted value up to the next , : ] } / \ " [ { ; = or #, trims it, and keeps it as a string
#   unless it is true, false or null in any case, or a number Java parses: 0.5f, True and a bare word all load.
#   Each such value is quoted; a value JSON already takes, such as 12 or false, is left as it is.
# Quoted strings are matched first and kept whole, so nothing inside one is taken for structure.
function Repair-JsonLeniency {

    param([string] $Text)

    $stringPattern = '"(?:[^"\\]|\\.)*"|''(?:[^''\\]|\\.)*'''
    $jsonValuePattern = '^(?:true|false|null|-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?)$'

    # The delimiter after a value is looked at, not taken, so it can open the next value.
    $bareValuePattern = '(?<lead>[:\[,]\s*)(?<value>[^\s"'',:\]}/\\\[{;=#][^",:\]}/\\\[{;=#\r\n]*?)(?=\s*[,\]}])'
    $separatorPattern = '(?<entrySeparator>;)|(?<keySeparator>=>?)'

    $rootEnd = -1
    $depth = 0

    foreach ($token in [regex]::Matches($Text, "$stringPattern|[{}\[\]]")) {

        switch ($token.Value) {

            { $_ -in '{', '[' } {
                $depth++
            }

            { $_ -in '}', ']' } {

                $depth--

                if ($depth -eq 0 -and $rootEnd -lt 0) {
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

        $value = $match.Groups['value'].Value

        if (-not $match.Groups['value'].Success -or $value -cmatch $jsonValuePattern) {
            return $match.Value
        }

        return "$($match.Groups['lead'].Value)`"$value`""
    }

    return [regex]::Replace($rootText, "$stringPattern|$separatorPattern|$bareValuePattern", $repairToken)
}
