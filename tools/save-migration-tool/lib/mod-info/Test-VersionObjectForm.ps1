# Says whether a version is in the { major, minor, patch } object form, rather than a string or bare number.
# Every reader of a version asks this one question, so a number or a part-less object never reads two ways.
function Test-VersionObjectForm {

    param([object] $Version)

    return $Version -is [System.Management.Automation.PSCustomObject]
}
