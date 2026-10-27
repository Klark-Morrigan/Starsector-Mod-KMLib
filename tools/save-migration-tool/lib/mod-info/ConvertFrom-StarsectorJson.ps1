# Parses JSON the way Starsector reads its data files: LoadingUtils strips # comments, then org.json parses.
# ConvertFrom-Json already takes the trailing commas and single-quoted strings org.json takes,
# and also takes // and /* */ comments, which org.json rejects, so those fail here as they fail in the game.
# org.json's leniencies ConvertFrom-Json lacks, such as an unquoted 0.5f or True, or text after the root,
# are repaired only when the text fails without them, because a rewrite can corrupt a file that did not need it.
# The game rejects a leading byte order mark, so this does too.
#
# Takes the text instead of the path when the caller has already read the file.
# With -AsHashtable, objects come back as ordered hashtables whose keys match case-sensitively, as org.json's do.
# Without it, keys differing only in case, which org.json keeps apart, fail the parse.
function ConvertFrom-StarsectorJson {

    [CmdletBinding(DefaultParameterSetName = 'Path')]
    param(
        [Parameter(Mandatory, ParameterSetName = 'Path')]
        [string] $Path,

        [Parameter(Mandatory, ParameterSetName = 'Text')]
        [AllowEmptyString()]
        [string] $Text,

        [switch] $AsHashtable
    )

    $byteOrderMark = [char] 0xFEFF

    if ($PSCmdlet.ParameterSetName -eq 'Path') {

        $source = $Path
        $Text = Read-StarsectorText -Path $Path

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
        return $json | ConvertFrom-Json -AsHashtable:$AsHashtable

    } catch {
        $strictError = $_.Exception.Message
    }

    try {
        return Repair-JsonLeniency -Text $json | ConvertFrom-Json -AsHashtable:$AsHashtable

    } catch {
        throw "not valid JSON, even with org.json's leniencies repaired: $source ($strictError)"
    }
}
