# The levels a change of mod version ranks at, after the game's VersionInfo.getCompatLevel.
# Named once, so the code that sets a level and the code that reads it agree.
function Get-VersionChangeLevelNames {

    return [pscustomobject]@{
        Major = 'Major'
        Minor = 'Minor'
        Patch = 'Patch'
    }
}
