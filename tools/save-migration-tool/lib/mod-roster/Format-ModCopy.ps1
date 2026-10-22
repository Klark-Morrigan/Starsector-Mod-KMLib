# Names one installed copy of a mod as its folder and its version, so a log line can say which copy won.
function Format-ModCopy {

    param([object] $Candidate)

    $folder = Format-ConsoleText -Role 'Path' -Text $Candidate.Folder.Name
    $version = Format-ConsoleText -Role 'Mod' -Text (Format-ModVersion -Version $Candidate.Version)

    return "$folder ($version)"
}
