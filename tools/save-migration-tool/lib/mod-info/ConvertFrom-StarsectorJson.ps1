# Parses JSON the way Starsector reads its data files: LoadingUtils strips # comments, then org.json parses.
# ConvertFrom-Json already takes the trailing commas and single-quoted strings org.json takes,
# and also takes // and /* */ comments, which org.json rejects, so those fail here as they fail in the game.
# org.json's one leniency ConvertFrom-Json lacks is the unquoted value, repaired only when the text fails without it,
# because a rewrite can corrupt a file that did not need it.
# The game rejects a leading byte order mark, so this does too.
#
# Takes the text instead of the path when the caller has already read the file.
function ConvertFrom-StarsectorJson {

    [CmdletBinding(DefaultParameterSetName = 'Path')]
    param(
        [Parameter(Mandatory, ParameterSetName = 'Path')]
        [string] $Path,

        [Parameter(Mandatory, ParameterSetName = 'Text')]
        [AllowEmptyString()]
        [string] $Text
    )

    # Unquoted bare word in value position, as in Thunder's Flag Pack's
    # "patch":a. true/false/null are excluded so real literals survive.
    $bareWordValuePattern = ':(\s*)(?!true\b|false\b|null\b)([A-Za-z_][\w.\-]*)(\s*[,}\]])'

    $byteOrderMark = [char] 0xFEFF

    if ($PSCmdlet.ParameterSetName -eq 'Path') {

        # GetString keeps a byte order mark as a character, where ReadAllText would drop it unseen.
        $source = $Path
        $Text = [System.Text.Encoding]::UTF8.GetString([System.IO.File]::ReadAllBytes($Path))

    } else {
        $source = 'the given text'
    }

    if ($Text.StartsWith($byteOrderMark)) {
        throw "starts with a byte order mark, which the game rejects: $source"
    }

    $json = Remove-JsonComment -Text $Text

    if (Test-JsonSlashComment -Text $json) {
        throw "holds a // or /* */ comment, which the game rejects: $source"
    }

    try {
        return $json | ConvertFrom-Json

    } catch {
        $strictError = $_.Exception.Message
    }

    try {
        return [regex]::Replace($json, $bareWordValuePattern, ':$1"$2"$3') | ConvertFrom-Json

    } catch {
        throw "not valid JSON, even with unquoted values quoted: $source ($strictError)"
    }
}
