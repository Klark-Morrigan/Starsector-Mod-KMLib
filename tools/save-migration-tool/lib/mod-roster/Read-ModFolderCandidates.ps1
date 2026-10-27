# Reads every folder under mods\ as a roster candidate, and names on stderr each folder holding no mod_info.json,
# which the game does not load as a mod.
function Read-ModFolderCandidates {

    [CmdletBinding()]
    param(
        [string] $ModsRoot,
        [string] $DiagnosticSource
    )

    $candidates = [System.Collections.Generic.List[object]]::new()
    $skippedFolders = [System.Collections.Generic.List[string]]::new()

    foreach ($folder in Get-ChildItem -LiteralPath $ModsRoot -Directory) {

        $candidate = Read-ModCandidate -Folder $folder

        if ($null -eq $candidate) {
            $skippedFolders.Add($folder.Name)

        } else {
            $candidates.Add($candidate)
        }
    }

    if ($skippedFolders.Count -gt 0) {

        $folderList = @($skippedFolders | ForEach-Object { Format-ConsoleText -Role 'Path' -Text $_ }) -join ', '
        $modInfoFileName = Format-ConsoleText -Role 'Path' -Text (Get-InstallPathNames).ModInfoFile

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "skipped $(Format-ConsoleNumber $skippedFolders.Count) folders with no ${modInfoFileName}: $folderList")
    }

    return ,$candidates.ToArray()
}
