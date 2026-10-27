# Reads the mod IDs enabled_mods.json says the game loads.
# IsListRead is false when the file is missing or holds no list.
# An unreadable list must not empty the roster, so the caller treats every installed mod as enabled,
# and a warning says so.
function Read-EnabledModIds {

    [CmdletBinding()]
    param([string] $ModsRoot)

    $enabledModsFileName = (Get-InstallPathNames).EnabledModsFile
    $enabledModsPropertyName = 'enabledMods'
    $unreadList = [pscustomobject]@{ Ids = @(); IsListRead = $false }

    $enabledFile = Join-Path $ModsRoot $enabledModsFileName

    if (-not (Test-Path -LiteralPath $enabledFile -PathType Leaf)) {

        Write-Warning "$enabledModsFileName not found, treating all mods as enabled."

        return $unreadList
    }

    try {

        # Read through the property, not Get-JsonPropertyValue: returning an empty list from a function unrolls it
        # to null, and an install with no mod enabled holds exactly that.
        $enabledProperty = (ConvertFrom-StarsectorJson -Path $enabledFile).PSObject.Properties[$enabledModsPropertyName]

        if ($null -eq $enabledProperty) {
            throw "it holds no $enabledModsPropertyName list"
        }

        return [pscustomobject]@{ Ids = @($enabledProperty.Value | Where-Object { $_ }); IsListRead = $true }

    } catch {

        Write-Warning "$enabledModsFileName unparseable, treating all mods as enabled: $($_.Exception.Message)"

        return $unreadList
    }
}
