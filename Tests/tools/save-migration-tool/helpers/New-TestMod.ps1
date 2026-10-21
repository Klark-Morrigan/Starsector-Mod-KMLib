# Writes mods\<folder>\mod_info.json under an install root with the given text, and returns the mod's folder.
function New-TestMod {

    param(
        [string] $StarsectorRoot,
        [string] $Folder,
        [string] $ModInfo
    )

    $modFolder = Join-Path $StarsectorRoot "mods\$Folder"

    New-Item -ItemType Directory -Path $modFolder -Force | Out-Null
    [System.IO.File]::WriteAllText((Join-Path $modFolder 'mod_info.json'), $ModInfo)

    return $modFolder
}
