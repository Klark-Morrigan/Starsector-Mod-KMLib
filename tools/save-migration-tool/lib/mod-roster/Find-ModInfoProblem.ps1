# Names the first thing in a parsed mod_info.json the game throws on, or null when it reads the whole file.
# The game skips a copy it throws on, so that copy never claims its mod ID.
# It requires "description", "id" and "name", in that order, then reads the versions and the dependencies.
function Find-ModInfoProblem {

    param([object] $Info)

    $requiredNames = @('description', 'id', 'name')
    $versionNames = @('version', 'gameVersion')

    foreach ($requiredName in $requiredNames) {

        if (-not (Test-JsonProperty -Object $Info -Name $requiredName)) {
            return "no `"$requiredName`" field"
        }
    }

    foreach ($versionName in $versionNames) {

        $versionProblem = Find-VersionProblem -Owner $Info -Name $versionName

        if ($versionProblem) {
            return $versionProblem
        }
    }

    return Find-ModDependencyProblem -Info $Info
}
