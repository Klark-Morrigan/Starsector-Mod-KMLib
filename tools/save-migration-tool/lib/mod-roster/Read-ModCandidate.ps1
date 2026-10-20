# Reads one mods\ folder as a roster candidate: its mod id, version and the key its version sorts by.
# Returns null for a folder with no mod_info.json, which the game does not load as a mod.
# Fails open: a mod_info.json nothing can parse still yields a candidate, under its folder name, with a warning.
# A filter that hides a real mod is worse than no filter, because nothing signals that it happened.
#   Depends on ConvertFrom-StarsectorJson, Get-JsonPropertyValue, Get-ModStatusNames and Get-VersionSortKey
#   being dot-sourced into the same scope.
function Read-ModCandidate {

    [CmdletBinding()]
    param([System.IO.DirectoryInfo] $Folder)

    $infoPath = Join-Path $Folder.FullName 'mod_info.json'

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

        Write-Warning "Unparseable mod_info.json, keeping in roster: $($Folder.Name) ($reason)"

        return [pscustomobject]@{
            Id      = $Folder.Name
            Version = $null
            SortKey = ''
            Folder  = $Folder
            Status  = $statuses.Unparsed
        }
    }

    $version = Get-JsonPropertyValue -Object $info -Name 'version'

    return [pscustomobject]@{
        Id      = [string] $modId
        Version = $version
        SortKey = Get-VersionSortKey -Version $version
        Folder  = $Folder
        Status  = $statuses.Parsed
    }
}
