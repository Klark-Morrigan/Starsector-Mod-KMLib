# Paints one side's version of a mod for a drift line, saying so when that side gives none.
function Format-ModDriftVersion {

    param([string] $Version)

    if (-not $Version) {
        return 'no version'
    }

    return Format-ConsoleText -Role 'Mod' -Text $Version
}
