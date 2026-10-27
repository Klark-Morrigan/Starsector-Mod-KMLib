# Names what the game throws on in a parsed mod_info.json's dependencies, or null when it reads them all.
# A mod may name no dependencies, but once it names them, the game requires a list of objects,
# each with an "id" and a version it reads.
function Find-ModDependencyProblem {

    param([object] $Info)

    $listName = 'dependencies'
    $idName = 'id'
    $versionName = 'version'

    if (-not (Test-JsonProperty -Object $Info -Name $listName)) {
        return $null
    }

    # Indexed rather than read through Get-JsonPropertyValue, whose return would unroll a one-item list.
    $dependencies = $Info[$listName]

    if ($dependencies -isnot [System.Collections.IList]) {
        return "`"$listName`" is not a list"
    }

    for ($index = 0; $index -lt $dependencies.Count; $index++) {

        $dependency = $dependencies[$index]
        $dependencyLabel = "dependency $($index + 1)"

        if ($dependency -isnot [System.Collections.IDictionary]) {
            return "$dependencyLabel is not an object"
        }

        if (-not (Test-JsonProperty -Object $dependency -Name $idName)) {
            return "no `"$idName`" in $dependencyLabel"
        }

        $versionProblem = Find-VersionProblem -Owner $dependency -Name $versionName

        if ($versionProblem) {
            return "$versionProblem of $dependencyLabel"
        }
    }

    return $null
}
