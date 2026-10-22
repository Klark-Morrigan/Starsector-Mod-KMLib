# Reads the mod IDs enabled_mods.json says the game loads.
# IsListRead is false when the file is missing or holds no list:
# an unreadable list must not empty the roster, so the caller treats every installed mod as enabled, and a warning
# says so.
function Read-EnabledModIds {

    [CmdletBinding()]
    param([string] $ModsRoot)

    $enabledFile = Join-Path $ModsRoot 'enabled_mods.json'

    if (-not (Test-Path -LiteralPath $enabledFile -PathType Leaf)) {

        Write-Warning 'enabled_mods.json not found, treating all mods as enabled.'
        return [pscustomobject]@{ Ids = @(); IsListRead = $false }
    }

    try {

        # Read through the property, not Get-JsonPropertyValue: returning an empty list from a function unrolls it
        # to null, and an install with no mod enabled holds exactly that.
        $enabledProperty = (ConvertFrom-StarsectorJson -Path $enabledFile).PSObject.Properties['enabledMods']

        if ($null -eq $enabledProperty) {
            throw 'it holds no enabledMods list'
        }

        return [pscustomobject]@{ Ids = @($enabledProperty.Value | Where-Object { $_ }); IsListRead = $true }

    } catch {

        Write-Warning "enabled_mods.json unparseable, treating all mods as enabled: $($_.Exception.Message)"
        return [pscustomobject]@{ Ids = @(); IsListRead = $false }
    }
}
