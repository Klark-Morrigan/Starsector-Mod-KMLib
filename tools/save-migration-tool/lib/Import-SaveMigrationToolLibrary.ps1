# Dot-sources every function under lib\ into the scope that dot-sources this file.
# A script dot-sources this one file, so it never lists the functions it calls or what those call in turn.
# The file holds no function of its own: a function's body runs in a scope of its own, which would lose them.
#
# A script one script runs sees its caller's functions already, and reloading them costs about half a second,
# so the marker this file leaves in the caller's scope skips the load for every script that caller runs.
$libraryRoot = $PSScriptRoot
$libraryMarkerName = 'SaveMigrationToolLibraryRoot'

if ((Get-Variable -Name $libraryMarkerName -ValueOnly -ErrorAction Ignore) -eq $libraryRoot) {
    return
}

foreach ($libraryFile in Get-ChildItem -LiteralPath $libraryRoot -Filter '*.ps1' -File -Recurse) {

    if ($libraryFile.FullName -ne $PSCommandPath) {
        . $libraryFile.FullName
    }
}

Set-Variable -Name $libraryMarkerName -Value $libraryRoot
