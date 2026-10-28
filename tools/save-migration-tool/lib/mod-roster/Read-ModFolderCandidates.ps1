# Reads every folder under mods\ as a roster candidate, and names on stderr each folder holding no mod_info.json,
# which the game does not load as a mod.
# Candidates come in the order the file system lists the folders, as Java's File.listFiles hands them to the game,
# because the game keeps the first copy of a mod it reads.
# Get-ChildItem would not do: it sorts by name, which puts "[" before letters where NTFS puts it after,
# and it skips hidden folders, which the game reads.
function Read-ModFolderCandidates {

    [CmdletBinding()]
    param(
        [string] $ModsRoot,
        [string] $DiagnosticSource
    )

    $candidates = [System.Collections.Generic.List[object]]::new()
    $skippedFolders = [System.Collections.Generic.List[string]]::new()

    # Get-Item resolves a path relative to PowerShell's location, where a bare DirectoryInfo would use the process's.
    foreach ($folder in (Get-Item -LiteralPath $ModsRoot).EnumerateDirectories()) {

        $candidate = Read-ModCandidate -Folder $folder

        if ($null -eq $candidate) {
            $skippedFolders.Add($folder.Name)

        } else {
            $candidates.Add($candidate)
        }
    }

    if ($skippedFolders.Count -gt 0) {

        $folderList = Format-ConsoleList -Role 'Path' -Values $skippedFolders
        $modInfoFileName = Format-ConsoleText -Role 'Path' -Text (Get-InstallPathNames).ModInfoFile

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "skipped $(Format-ConsoleNumber $skippedFolders.Count) folders with no ${modInfoFileName}: $folderList")
    }

    return ,$candidates.ToArray()
}
