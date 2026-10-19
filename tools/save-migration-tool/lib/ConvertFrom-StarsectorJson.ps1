# Parses a JSON file the way Starsector's reader accepts it.
# The game takes input that ConvertFrom-Json rejects:
# BOMs, line comments, trailing commas, single-quoted scalars and bare unquoted words.
#
# Repairs are tried least-invasive first and never applied speculatively,
# because a rewrite can corrupt a file that did not need it.
# The single-quote pass is the cautionary case: it reads a pair of apostrophes in prose as a quoted scalar,
# which breaks Magellan Protectorate - a file that parses cleanly without it.
#   Depends on Remove-JsonComment being dot-sourced into the same scope.
function ConvertFrom-StarsectorJson {

    param([string] $Path)

    # Single-quoted scalar in value position: MagicLib and apex_design both write
    # "major": '1' inside their version object.
    $singleQuotedValuePattern = "(?<=[:\[,{]\s{0,8})'([^']*)'(?=\s{0,8}[,\]}])"

    # Unquoted bare word in value position, as in Thunder's Flag Pack's
    # "patch":a. true/false/null are excluded so real literals survive.
    $bareWordValuePattern = ':(\s*)(?!true\b|false\b|null\b)([A-Za-z_][\w.\-]*)(\s*[,}\]])'

    $raw = [System.IO.File]::ReadAllText($Path)
    $raw = Remove-JsonComment -Text $raw
    $raw = [regex]::Replace($raw, ',(\s*[}\]])', '$1')

    $singleQuoted = [regex]::Replace($raw, $singleQuotedValuePattern, '"$1"')
    $bareWord = [regex]::Replace($raw, $bareWordValuePattern, ':$1"$2"$3')
    $bothRepairs = [regex]::Replace($singleQuoted, $bareWordValuePattern, ':$1"$2"$3')

    $lastError = $null

    foreach ($candidate in @($raw, $singleQuoted, $bareWord, $bothRepairs)) {

        try {
            return $candidate | ConvertFrom-Json

        } catch {
            # The next, more invasive repair gets its turn; the throw below reports when none parses.
            $lastError = $_.Exception.Message
        }
    }

    throw "no repair produced valid JSON: $Path ($lastError)"
}
