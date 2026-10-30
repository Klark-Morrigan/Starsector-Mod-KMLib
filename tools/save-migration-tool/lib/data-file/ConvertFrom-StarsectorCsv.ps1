# Reads a spreadsheet the way the game's own CSV reader does, neither stricter nor looser.
# Import-Csv refuses a header that repeats a column name, as several mods' headers do, and the game does not.
# Returns the header's column index by name, and every row the game keeps, in file order, each an array of its values.
# Every column is read, so several readers needing different columns parse the file once;
# Select-CsvColumns picks out the ones a reader needs.
#
# The game's rules, which a mod's file relies on:
# - The first line is the header. It splits on every comma, quoted or not; each name is trimmed and loses one
#   enclosing quote. A name the header repeats takes the value of its last column.
# - A row that opens with # is a comment, and an empty line is no row.
# - A double quote turns quoting on or off; a comma or line break inside quotes is text.
#   Two double quotes in a row are one literal quote, inside quotes or out, so "" is a quote, not an empty value.
# - Values are not trimmed. Carriage returns are dropped.
# - rules.csv has its curly quotes straightened before it is read, as ConvertTo-StraightQuotes describes.
#
# Takes the text instead of the path when the caller has already read the file.
function ConvertFrom-StarsectorCsv {

    [CmdletBinding(DefaultParameterSetName = 'Path')]
    param(
        [Parameter(Mandatory, ParameterSetName = 'Path')]
        [string] $Path,

        [Parameter(Mandatory, ParameterSetName = 'Text')]
        [AllowEmptyString()]
        [string] $Text,

        [switch] $ConvertsCurlyQuotes
    )

    # A field is a run of quoted sections, "" pairs and plain text. The groups are atomic: a field has one
    # reading, so a quote left open fails the row straight away instead of retrying every split.
    $fieldPattern = '(?>(?:""|"(?>(?:""|[^"]+)*)"|[^",\n]+)*)'

    # One row: its fields, each kept as a capture of the one group, then the line break that ends it.
    $rowPattern = "\G(?:(?<field>$fieldPattern),)*(?<field>$fieldPattern)\n"

    # Each "" pair is a literal quote, and every other quote only turns quoting on or off.
    $quotePattern = '"("?)'

    $lineFeed = "`n"
    $headerQuote = '"'
    $commentMark = '#'

    if ($PSCmdlet.ParameterSetName -eq 'Path') {
        $Text = Read-StarsectorText -Path $Path
    }

    if ($ConvertsCurlyQuotes) {
        $Text = ConvertTo-StraightQuotes -Text $Text
    }

    $columnIndexByName = [System.Collections.Generic.Dictionary[string, int]]::new()
    $body = $Text.Replace("`r", '')
    $headerEnd = $body.IndexOf($lineFeed)

    # The game reads no rows from a file of one line.
    if ($headerEnd -lt 0 -or $body.Length -le $headerEnd + 1) {
        return [pscustomobject]@{ ColumnIndexByName = $columnIndexByName; Rows = [object[]] @() }
    }

    $headerNames = $body.Substring(0, $headerEnd).Split(',')

    for ($index = 0; $index -lt $headerNames.Count; $index++) {

        $name = $headerNames[$index].Trim()

        if ($name.StartsWith($headerQuote)) {
            $name = $name.Substring(1)
        }

        if ($name.EndsWith($headerQuote)) {
            $name = $name.Substring(0, $name.Length - 1)
        }

        $columnIndexByName[$name.Replace('""', '"')] = $index
    }

    # The game ends the last row with a line break of its own.
    $rows = $body.Substring($headerEnd + 1) + $lineFeed
    $rowEnd = 0

    $values = @(foreach ($row in [regex]::Matches($rows, $rowPattern)) {

        $rowEnd = $row.Index + $row.Length

        if ($row.Value -eq $lineFeed -or $row.Value.StartsWith($commentMark)) {
            continue
        }

        # The operator, unlike a method call, costs little enough to run on every field of a large spreadsheet.
        ,[string[]] @(foreach ($field in $row.Groups['field'].Captures) { $field.Value -replace $quotePattern, '$1' })
    })

    # Every row the game reads ends in a line break, so rows stop short only at a quote left open.
    if ($rowEnd -ne $rows.Length) {
        throw "has a quote left open, at character $rowEnd after the header"
    }

    return [pscustomobject]@{ ColumnIndexByName = $columnIndexByName; Rows = $values }
}
