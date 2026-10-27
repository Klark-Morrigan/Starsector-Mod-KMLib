# Reads one mods\ folder as a roster candidate: its mod ID, name, version and the JARs it declares,
# as mod_info.json writes them.
# It also reads what decides how the game reads the mod's data: the text the game sorts mods by, and
# the data files the mod replaces outright. The game ignores a replaced settings.json, so those are left out.
# Returns null for a folder with no mod_info.json, which the game does not load as a mod.
# Fails open: a mod_info.json nothing can parse, or one naming no ID, still yields a candidate,
# under its folder name, with a warning.
# A filter that hides a real mod is worse than no filter, because nothing signals that it happened.
function Read-ModCandidate {

    [CmdletBinding()]
    param([System.IO.DirectoryInfo] $Folder)

    $modInfoFileName = (Get-InstallPathNames).ModInfoFile
    $infoPath = Join-Path $Folder.FullName $modInfoFileName
    $ignoredReplacementEnding = 'settings.json'

    if (-not (Test-Path -LiteralPath $infoPath -PathType Leaf)) {
        return $null
    }

    $statuses = Get-ModStatusNames
    $info = $null
    $parseError = $null

    try {
        $info = ConvertFrom-StarsectorJson -Path $infoPath

    } catch {
        $parseError = $_.Exception.Message
    }

    $modId = Get-JsonPropertyValue -Object $info -Name 'id'

    if (-not $modId) {

        $reason = if ($parseError) { $parseError } else { 'no "id" field' }

        Write-Warning "No mod ID read from $modInfoFileName, keeping in roster: $($Folder.Name) ($reason)"

        return [pscustomobject]@{
            Id            = $Folder.Name
            Name          = $null
            Version       = $null
            Jars          = @()
            SortString    = $null
            ReplacedPaths = @()
            Folder        = $Folder
            Status        = $statuses.Unparsed
        }
    }

    $version = Get-JsonPropertyValue -Object $info -Name 'version'
    $replacedPaths = @(Get-JsonPropertyValue -Object $info -Name 'replace' | Where-Object {
        $_ -and -not "$_".EndsWith($ignoredReplacementEnding, [System.StringComparison]::Ordinal)
    })

    return [pscustomobject]@{
        Id            = [string] $modId
        Name          = Get-JsonPropertyValue -Object $info -Name 'name'
        Version       = $version
        Jars         = @(Get-JsonPropertyValue -Object $info -Name 'jars' | Where-Object { $_ })
        SortString    = Get-JsonPropertyValue -Object $info -Name 'sortString'
        ReplacedPaths = $replacedPaths
        Folder        = $Folder
        Status        = $statuses.Parsed
    }
}
