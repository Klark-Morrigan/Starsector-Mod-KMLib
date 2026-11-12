# One installed copy of a mod, as Read-ModCandidate reads it from its folder.
# A copy whose mod_info.json the game skips has only its ID, folder and status; every other field stays empty.
function New-ModCandidate {

    param(
        # The "id" in mod_info.json, or the folder name when nothing could be read.
        [string] $Id,

        # Untyped so a field mod_info.json leaves out stays null, not empty.
        [object] $Name = $null,
        [object] $Version = $null,
        [object] $ModPlugin = $null,
        [object] $SortString = $null,

        # As mod_info.json writes them, relative to the folder.
        [object[]] $Jars = @(),
        [object[]] $ReplacedPaths = @(),

        [System.IO.DirectoryInfo] $Folder,

        # One of Get-ModStatusNames.
        [string] $Status
    )

    return [pscustomobject]@{
        Id = $Id
        Name = $Name
        Version = $Version
        Jars = $Jars
        ModPlugin = $ModPlugin
        SortString = $SortString
        ReplacedPaths = $ReplacedPaths
        Folder = $Folder
        Status = $Status
    }
}
