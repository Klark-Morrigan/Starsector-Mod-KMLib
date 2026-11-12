# Reads the mod IDs enabled_mods.json says the game loads, as ModManager.loadEnabledModList reads them.
# A missing or unparsable file, or one with no enabledMods list, gives no IDs, with a warning:
# the game then loads no mods, and a roster listing them all would hide that a broken list is why a save fails.
# The game stops at an empty entry, as in ["a",,"b"], and keeps the IDs before it.
# ConvertFrom-Json reads a literal null as it reads an empty entry, so a null stops the read here too,
# where the game reads it as the ID "null" and goes on.
# The IDs come back one by one, so a caller collects them with @(...): no IDs then reads as an empty list, not null.
function Read-EnabledModIds {

    [CmdletBinding()]
    param([string] $ModsRoot)

    $enabledModsFileName = (Get-InstallPathNames).EnabledModsFile
    $enabledModsPropertyName = 'enabledMods'

    $enabledFile = Join-Path `
        $ModsRoot `
        $enabledModsFileName

    if (-not (Test-Path `
            -LiteralPath $enabledFile `
            -PathType Leaf)) {

        Write-Warning "$enabledModsFileName not found, so the game loads no mods."
        return
    }

    try {

        $enabledMods = ConvertFrom-StarsectorJson -Path $enabledFile

        # Index the list, not Get-JsonPropertyValue: returning an empty list from a function unrolls it to null,
        # and an install with no mod enabled holds exactly that.
        $isListHeld = $enabledMods -is [System.Collections.IDictionary] `
            -and $enabledMods.Contains($enabledModsPropertyName) `
            -and $enabledMods[$enabledModsPropertyName] -is [System.Collections.IList]

        if (-not $isListHeld) {
            throw "it holds no $enabledModsPropertyName list"
        }

        $entries = $enabledMods[$enabledModsPropertyName]

    } catch {

        Write-Warning "$enabledModsFileName unparsable, so the game loads no mods: $($_.Exception.Message)"
        return
    }

    $ids = [System.Collections.Generic.List[string]]::new()

    for ($index = 0; $index -lt $entries.Count; $index++) {

        if ($null -eq $entries[$index]) {

            Write-Warning (
                "$enabledModsFileName has an empty entry at position $($index + 1), " +
                'so the game loads only the IDs before it.')

            break
        }

        # A blank ID names no mod.
        $id = [string] $entries[$index]

        if (-not [string]::IsNullOrEmpty($id)) {
            $ids.Add($id)
        }
    }

    return $ids.ToArray()
}
