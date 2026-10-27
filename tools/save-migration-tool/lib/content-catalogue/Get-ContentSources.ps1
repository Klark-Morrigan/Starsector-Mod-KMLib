# Lists the folders the game reads data from, in the order it searches them: the mods, then the game's own.
# The game sorts the mods by each mod_info.json's sortString, or by its name when it gives none,
# comparing the text code unit by code unit. Mods that sort alike keep the roster's order.
# How the game combines the copies of one file depends on the file, and on whether a copy is a mod's.
#
# A mod the roster keeps as unparsed has no name, so it sorts by its folder name.
# Its content still counts: the mod is installed, and calling its content undefined would mark it for removal.
function Get-ContentSources {

    param(
        [string] $StarsectorRoot,

        # The install's enabled mods, as Read-ModRoster lists them.
        [object[]] $Roster
    )

    $sortTextSelector = [System.Func[object, string]] {

        param($mod)

        if ($mod.SortString) {
            return $mod.SortString
        }

        if ($mod.Name) {
            return $mod.Name
        }

        return $mod.Id
    }

    # OrderBy keeps the order of mods that sort alike, as the game's own sort does.
    $modsInLoadOrder = [System.Linq.Enumerable]::OrderBy(
        [object[]] $Roster,
        $sortTextSelector,
        [System.StringComparer]::Ordinal)

    $sources = @(foreach ($mod in $modsInLoadOrder) {

        [pscustomobject]@{
            Owner         = $mod.Id
            Name          = $mod.Name
            Version       = $mod.Version
            Folder        = $mod.Folder
            IsMod         = $true
            ReplacedPaths = $mod.ReplacedPaths
        }
    })

    # The game's own data has no mod_info.json to name it or give its version.
    $coreSource = [pscustomobject]@{
        Owner         = Get-CoreOwnerName
        Name          = 'Starsector'
        Version       = $null
        Folder        = Join-Path $StarsectorRoot (Get-InstallPathNames).CoreFolder
        IsMod         = $false
        ReplacedPaths = @()
    }

    return $sources + $coreSource
}
