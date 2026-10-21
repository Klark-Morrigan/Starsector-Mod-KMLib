# Dot-sources every function under lib\ into the scope that dot-sources this file.
# A script dot-sources this one file, so it never lists the functions it calls or what those call in turn.
# The file holds no function of its own: a function's body runs in a scope of its own, which would lose them.
foreach ($libraryFile in Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.ps1' -File -Recurse) {

    if ($libraryFile.FullName -ne $PSCommandPath) {
        . $libraryFile.FullName
    }
}
