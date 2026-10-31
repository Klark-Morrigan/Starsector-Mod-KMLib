# The one comparison every mod ID match goes through: ordinal, so case-sensitive, as the game's String.equals is.
# The game keeps two IDs differing only in case apart wherever it meets them: in enabled_mods.json,
# among the copies under mods\ and in a save's mod lists.
function Get-ModIdComparer {

    return [System.StringComparer]::Ordinal
}
