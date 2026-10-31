# Describes one folder the game reads data from: whose it is, where it is, and the paths it replaces.
function New-ContentSource {

    param(
        # The mod ID, or Get-CoreOwnerName for the game's own data.
        [string] $Owner,

        # Untyped so a missing name or version stays null, as the catalogue's JSON writes it, not empty.
        [object] $Name,
        [object] $Version,

        [string] $Folder,

        [switch] $IsMod,

        # The paths mod_info.json lists under "replace", relative to the folder.
        [string[]] $ReplacedPaths = @()
    )

    return [pscustomobject]@{
        Owner         = $Owner
        Name          = $Name
        Version       = $Version
        Folder        = $Folder
        IsMod         = [bool] $IsMod
        ReplacedPaths = $ReplacedPaths
    }
}
