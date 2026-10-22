# Paints a number, so the figures in a line stand out from its words.
function Format-ConsoleNumber {

    param([object] $Number)

    return Format-ConsoleText -Role 'Number' -Text "$Number"
}
