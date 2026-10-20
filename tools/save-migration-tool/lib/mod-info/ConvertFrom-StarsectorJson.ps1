# Parses JSON the way Starsector reads its data files: LoadingUtils strips # comments, then org.json parses.
# org.json accepts trailing commas, single-quoted strings and unquoted values, which ConvertFrom-Json rejects.
# The game rejects a leading byte order mark, so this does too.
#
# The repairs that stand in for org.json's leniency are tried least-invasive first, and each only once the one
# before it failed, because a rewrite can corrupt a file that did not need it.
# The single-quote pass is the cautionary case: it reads a pair of apostrophes in prose as a quoted scalar,
# which breaks Magellan Protectorate - a file that parses cleanly without it.
#
# Takes the text instead of the path when the caller has already read the file.
#   Depends on Remove-JsonComment being dot-sourced into the same scope.
function ConvertFrom-StarsectorJson {

    [CmdletBinding(DefaultParameterSetName = 'Path')]
    param(
        [Parameter(Mandatory, ParameterSetName = 'Path')]
        [string] $Path,

        [Parameter(Mandatory, ParameterSetName = 'Text')]
        [AllowEmptyString()]
        [string] $Text
    )

    # Single-quoted scalar in value position: MagicLib and apex_design both write
    # "major": '1' inside their version object.
    $singleQuotedValuePattern = "(?<=[:\[,{]\s{0,8})'([^']*)'(?=\s{0,8}[,\]}])"

    # Unquoted bare word in value position, as in Thunder's Flag Pack's
    # "patch":a. true/false/null are excluded so real literals survive.
    $bareWordValuePattern = ':(\s*)(?!true\b|false\b|null\b)([A-Za-z_][\w.\-]*)(\s*[,}\]])'

    $byteOrderMark = [char] 0xFEFF
    $source = if ($PSCmdlet.ParameterSetName -eq 'Path') { $Path } else { 'the given text' }

    # GetString keeps a byte order mark as a character, where ReadAllText would drop it unseen.
    if ($PSCmdlet.ParameterSetName -eq 'Path') {
        $Text = [System.Text.Encoding]::UTF8.GetString([System.IO.File]::ReadAllBytes($Path))
    }

    if ($Text.StartsWith($byteOrderMark)) {
        throw "starts with a byte order mark, which the game rejects: $source"
    }

    $raw = Remove-JsonComment -Text $Text
    $raw = [regex]::Replace($raw, ',(\s*[}\]])', '$1')

    $repairs = @(
        { param($json) $json }
        { param($json) [regex]::Replace($json, $singleQuotedValuePattern, '"$1"') }
        { param($json) [regex]::Replace($json, $bareWordValuePattern, ':$1"$2"$3') }
        {
            param($json)
            $singleQuoted = [regex]::Replace($json, $singleQuotedValuePattern, '"$1"')
            [regex]::Replace($singleQuoted, $bareWordValuePattern, ':$1"$2"$3')
        }
    )

    $lastError = $null

    foreach ($repair in $repairs) {

        try {
            return (& $repair $raw) | ConvertFrom-Json

        } catch {
            # The next, more invasive repair gets its turn; the throw below reports when none parses.
            $lastError = $_.Exception.Message
        }
    }

    throw "no repair produced valid JSON: $source ($lastError)"
}
