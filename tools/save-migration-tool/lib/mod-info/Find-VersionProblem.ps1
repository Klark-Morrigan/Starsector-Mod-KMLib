# Names what the game throws on in the version an object holds under a key, or null when it reads the version.
# The game requires a version object's "major", and reads any other value, or none, as text.
function Find-VersionProblem {

    param(
        [object] $Owner,
        [string] $Name
    )

    $majorName = 'major'
    $version = Get-JsonPropertyValue -Object $Owner -Name $Name
    $isMajorMissing = (Test-VersionObjectForm -Version $version) -and
        -not (Test-JsonProperty -Object $version -Name $majorName)

    if ($isMajorMissing) {
        return "no `"$majorName`" in `"$Name`""
    }

    return $null
}
