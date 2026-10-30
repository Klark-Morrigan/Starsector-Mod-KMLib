# Reads the given columns of a spreadsheet the way the game's own CSV reader does, neither stricter nor looser.
# Import-Csv refuses a header that repeats a column name, as several mods' headers do, and the game does not.
# Returns one array per row the game keeps, in file order, holding each asked column's value in the order asked,
# or null where the header has no such column or the row ends before it. The rows come as one array, empty or not.
#
# The game's rules, which a mod's file relies on:
# - The first line is the header. It splits on every comma, quoted or not; each name is trimmed and loses one
#   enclosing quote. A name the header repeats takes the value of its last column.
# - A row that opens with # is a comment, and an empty line is no row.
# - A double quote turns quoting on or off; a comma or line break inside quotes is text.
#   Two double quotes in a row are one literal quote, inside quotes or out, so "" is a quote, not an empty value.
# - Values are not trimmed. Carriage returns are dropped.
function ConvertFrom-StarsectorCsv {

    param(
        [AllowEmptyString()]
        [string] $Text,

        [string[]] $ColumnNames
    )

    # A field is a run of quoted sections, "" pairs and plain text. The groups are atomic: a field has one
    # reading, so a quote left open fails the row straight away instead of retrying every split.
    $fieldPattern = '(?>(?:""|"(?>(?:""|[^"]+)*)"|[^",\n]+)*)'

    # One row: its fields, each kept as a capture of the one group, then the line break that ends it.
    $rowPattern = "\G(?:(?<field>$fieldPattern),)*(?<field>$fieldPattern)\n"

    $lineFeed = "`n"
    $headerQuote = '"'
    $commentMark = '#'

    $body = $Text.Replace("`r", '')
    $headerEnd = $body.IndexOf($lineFeed)

    # The game reads no rows from a file of one line.
    if ($headerEnd -lt 0 -or $body.Length -le $headerEnd + 1) {
        return ,@()
    }

    $columnIndexes = [int[]]::new($ColumnNames.Count)
    $headerNames = $body.Substring(0, $headerEnd).Split(',')

    for ($column = 0; $column -lt $ColumnNames.Count; $column++) {
        $columnIndexes[$column] = -1
    }

    for ($index = 0; $index -lt $headerNames.Count; $index++) {

        $name = $headerNames[$index].Trim()

        if ($name.StartsWith($headerQuote)) {
            $name = $name.Substring(1)
        }

        if ($name.EndsWith($headerQuote)) {
            $name = $name.Substring(0, $name.Length - 1)
        }

        $name = $name.Replace('""', '"')

        for ($column = 0; $column -lt $ColumnNames.Count; $column++) {

            if ($name -ceq $ColumnNames[$column]) {
                $columnIndexes[$column] = $index
            }
        }
    }

    # The game ends the last row with a line break of its own.
    $rows = $body.Substring($headerEnd + 1) + $lineFeed
    $values = [System.Collections.Generic.List[object[]]]::new()
    $rowEnd = 0

    foreach ($row in [regex]::Matches($rows, $rowPattern)) {

        $rowEnd = $row.Index + $row.Length

        if ($row.Value -eq $lineFeed -or $row.Value.StartsWith($commentMark)) {
            continue
        }

        $fields = $row.Groups['field'].Captures
        $rowValues = [object[]]::new($ColumnNames.Count)

        for ($column = 0; $column -lt $ColumnNames.Count; $column++) {

            $index = $columnIndexes[$column]

            # Each "" pair is a literal quote, and every other quote only turns quoting on or off.
            if ($index -ge 0 -and $index -lt $fields.Count) {
                $rowValues[$column] = [regex]::Replace($fields[$index].Value, '"("?)', '$1')
            }
        }

        $values.Add($rowValues)
    }

    # Every row the game reads ends in a line break, so rows stop short only at a quote left open.
    if ($rowEnd -ne $rows.Length) {
        throw "has a quote left open, at character $rowEnd after the header"
    }

    return ,$values.ToArray()
}
