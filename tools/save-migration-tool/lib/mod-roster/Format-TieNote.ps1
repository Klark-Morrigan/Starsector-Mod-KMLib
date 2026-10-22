# Says what decided between two copies of a mod when their version numbers tie, as the end of a log line:
# the version as written, or, at the very same version, the folder name. Says nothing when the numbers differ.
# Group-ModCandidate orders the copies by these same three keys.
function Format-TieNote {

    param(
        [object] $Winner,
        [object] $Loser
    )

    if ($Winner.SortKey -ne $Loser.SortKey) {
        return ''
    }

    if ((Format-ModVersion -Version $Winner.Version) -cne (Format-ModVersion -Version $Loser.Version)) {
        return ', the same version number, so the higher version as written'
    }

    return ', the same version, so first by folder name'
}
