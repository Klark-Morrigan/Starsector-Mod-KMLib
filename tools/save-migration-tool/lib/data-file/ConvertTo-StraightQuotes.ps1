# Turns curly quotes into straight ones, as the game does to a spreadsheet's text before it reads rules.csv.
# A run of curly double quotes becomes one double quote, which then opens or closes quoting,
# and a run of curly single quotes or replacement characters becomes one apostrophe.
function ConvertTo-StraightQuotes {

    param([string] $Text)

    $straightened = [regex]::Replace($Text, '[\u201C\u201D]+', '"')

    return [regex]::Replace($straightened, '[\u2018\u2019\uFFFD]+', "'")
}
