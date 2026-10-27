# Reads the ID one copy of a content file sets at its top level: the hull of a .ship, the weapon of a .wpn, and so on.
# Returns null when the copy does not set it, as a mod's copy that patches a few keys of another's does not.
# Throws when the copy does not parse.
#
# An install holds tens of thousands of these files, and parsing each in full takes minutes,
# so the ID is taken by pattern when the key appears once with a plain string value, and absent when it never appears.
# Anything else, such as a key written twice or a value the pattern cannot read, gets the full parse the game makes.
# The key is matched however org.json takes it: double-quoted, single-quoted or bare, as the game's own .faction
# files write it, and followed by a colon, = or =>.
function Read-ContentFileId {

    param(
        [string] $Path,
        [string] $IdKey
    )

    $byteOrderMark = [char] 0xFEFF
    $escapedKey = [regex]::Escape($IdKey)

    # A bare key is a whole word, so id never matches inside proj_id or a longer name.
    $keyPattern = '(?:"{0}"|''{0}''|(?<![\w"''.$-]){0}(?![\w.$-]))\s*(?:=>?|:)\s*' -f $escapedKey

    # A plain string value holds no escape, which the pattern could not decode as JSON does.
    $plainValuePattern = $keyPattern + '"(?<id>[^"\\]*)"'

    $text = Read-StarsectorText -Path $Path

    # The full parse refuses a byte order mark, as the game does.
    if (-not $text.StartsWith($byteOrderMark)) {

        $json = Remove-JsonComment -Text $text
        $keyCount = [regex]::Matches($json, $keyPattern).Count

        if ($keyCount -eq 0) {
            return $null
        }

        $plainValue = [regex]::Match($json, $plainValuePattern)

        if ($keyCount -eq 1 -and $plainValue.Success) {
            return $plainValue.Groups['id'].Value
        }
    }

    $id = Get-JsonPropertyValue -Object (ConvertFrom-StarsectorJson -Text $text) -Name $IdKey

    if ($null -ne $id -and $id -isnot [string]) {
        throw "its $IdKey is not text"
    }

    return $id
}
