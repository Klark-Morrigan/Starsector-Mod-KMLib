# Paints a value in its role's colour from the palette, resetting after it.
# Painting does not decide whether the colour shows: Resolve-ConsoleText strips it where the stream is not a console.
function Format-ConsoleText {

    param(
        [string] $Role,
        [string] $Text
    )

    $palette = Get-ConsolePalette

    if (-not $palette.ContainsKey($Role)) {
        throw "no console colour for role $Role"
    }

    return "$($palette[$Role])$Text$($palette.Reset)"
}
