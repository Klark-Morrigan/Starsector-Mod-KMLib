# The path of one file of a save, in the save's folder under saves\.
function Get-SaveFilePath {

    param(
        [string] $StarsectorRoot,

        # The save's folder name under saves\.
        [string] $SaveFolder,

        # The file's name, as Get-InstallPathNames names the save's files.
        [string] $FileName
    )

    return Join-Path `
        -Path $StarsectorRoot `
        -ChildPath (Get-InstallPathNames).SavesFolder `
        -AdditionalChildPath $SaveFolder, $FileName
}
