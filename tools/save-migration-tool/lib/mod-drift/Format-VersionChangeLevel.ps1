# Names the level of a version change for a person: a major change red and a minor one amber,
# as the game's load list warns of them. A patch change stays unpainted,
# so the changes that need a look stand out from the many small ones.
function Format-VersionChangeLevel {

    param([string] $Level)

    $levels = Get-VersionChangeLevelNames

    if ($Level -eq $levels.Major) {
        return Format-ConsoleText -Role 'Error' -Text 'major change'
    }

    if ($Level -eq $levels.Minor) {
        return Format-ConsoleText -Role 'Warning' -Text 'minor change'
    }

    return 'patch change'
}
