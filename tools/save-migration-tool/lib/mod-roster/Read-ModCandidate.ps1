# Reads one mods\ folder as a roster candidate: its mod ID, name, version, the JARs it declares
# and the class it names as its mod plugin, as mod_info.json writes them.
# It also reads what decides how the game reads the mod's data: the text the game sorts mods by, and
# the data files the mod replaces outright. The game ignores a replaced settings.json, so those are left out.
# Returns null for a folder with no mod_info.json, which the game does not load as a mod.
# Fails open: a mod_info.json the game skips, as nothing parses it or it lacks a field the game requires,
# still yields a candidate, under its folder name, with a warning.
# A filter that hides a real mod is worse than no filter, because nothing signals that it happened.
function Read-ModCandidate {

    [CmdletBinding()]
    param([System.IO.DirectoryInfo] $Folder)

    $modInfoFileName = (Get-InstallPathNames).
        ModInfoFile

    $infoPath = Join-Path `
        $Folder.FullName `
        $modInfoFileName

    $ignoredReplacementEnding = 'settings.json'

    if (-not (Test-Path `
            -LiteralPath $infoPath `
            -PathType Leaf)) {

        return $null
    }

    $statuses = Get-ModStatusNames
    $info = $null
    $parseError = $null

    try {
        $info = ConvertFrom-StarsectorJson `
            -Path $infoPath

    } catch {
        $parseError = $_.Exception.Message
    }

    $problem = if ($parseError) {
        $parseError
    } else {
        Find-ModInfoProblem -Info $info
    }

    if ($problem) {

        Write-Warning "No mod read from $modInfoFileName, keeping in roster: $($Folder.Name) ($problem)"

        return New-ModCandidate `
            -Id $Folder.Name `
            -Folder $Folder `
            -Status $statuses.Unparsed
    }

    $modId = Get-JsonPropertyValue `
        -Object $info `
        -Name 'id'

    $version = Get-JsonPropertyValue `
        -Object $info `
        -Name 'version'

    $replacedPaths = @(Get-JsonPropertyValue `
        -Object $info `
        -Name 'replace' |
        Where-Object {
            $_ `
                -and -not "$_".EndsWith(
                    $ignoredReplacementEnding,
                    [System.StringComparison]::Ordinal)
    })

    $name = Get-JsonPropertyValue `
        -Object $info `
        -Name 'name'

    $jars = @(Get-JsonPropertyValue `
        -Object $info `
        -Name 'jars' |
        Where-Object { $_ })

    $modPlugin = Get-JsonPropertyValue `
        -Object $info `
        -Name 'modPlugin'

    $sortString = Get-JsonPropertyValue `
        -Object $info `
        -Name 'sortString'

    return New-ModCandidate `
        -Id ([string] $modId) `
        -Name $name `
        -Version $version `
        -Jars $jars `
        -ModPlugin $modPlugin `
        -SortString $sortString `
        -ReplacedPaths $replacedPaths `
        -Folder $Folder `
        -Status $statuses.Parsed
}
