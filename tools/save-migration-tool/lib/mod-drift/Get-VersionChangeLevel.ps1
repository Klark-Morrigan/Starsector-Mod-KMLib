# Ranks the change between a save's version of a mod and the installed one, for versions whose text differs.
# It follows the game's load list, VersionInfo.getCompatLevel: the first of major, then minor, that differs names
# the level. Parts compare as exact text, not as numbers. A minor part both sides mark as a release candidate is not
# compared.
# Any other change is a patch, including those the game reads as no change ("1.0.2" and "1.0.2a" split into the same
# parts) or cannot rank (a side with no major part): the text changed, so the content may have too.
# The load list warns of a major change in red, and of a minor or patch change in yellow.
function Get-VersionChangeLevel {

    param(
        [object] $Installed,
        [object] $Saved
    )

    $levels = Get-VersionChangeLevelNames

    if (-not $Installed.Major -or -not $Saved.Major) {
        return $levels.Patch
    }

    if ($Installed.Major -cne $Saved.Major) {
        return $levels.Major
    }

    $isMinorCompared = -not ($Installed.MinorIsRC -and $Saved.MinorIsRC)

    if ($isMinorCompared -and $Installed.Minor -cne $Saved.Minor) {
        return $levels.Minor
    }

    return $levels.Patch
}
