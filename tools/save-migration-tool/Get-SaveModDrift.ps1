#requires -Version 7.0
<#
.SYNOPSIS
    Compares the mods a save was last written with against the installed ones: changed, missing, added.

.DESCRIPTION
    The save's side comes from its descriptor, the small file beside campaign.xml that the game's load list reads.
    The installed side is Get-ModRoster.ps1's roster of the enabled mods.
    A changed mod's level, major, minor or patch, follows the rule the game's load list warns by.
    Stdout carries one row per mod on either side. What the script read and decided goes to stderr.
    The README beside this script explains how the descriptor stores the list and how versions compare.

.EXAMPLE
    Get-SaveModDrift.ps1 -StarsectorRoot <install> -SaveFolder <save folder> -AsJson
    Compares the save's mods with the installed ones, as JSON.
#>

[CmdletBinding()]
param(
    # Starsector install root; the save and the mods are resolved beneath it.
    [Parameter(Mandatory)]
    [string] $StarsectorRoot,

    # The save's folder name under saves\, as Get-SaveLoadAttempt.ps1 reports it.
    [Parameter(Mandatory)]
    [string] $SaveFolder,

    # Print JSON instead of objects, for a caller that parses the output.
    [switch] $AsJson
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\Import-SaveMigrationToolLibrary.ps1')

# Six columns would print as a list, so the rows get a table view of their own.
Update-FormatData -PrependPath (Join-Path $PSScriptRoot 'formats\ModDriftRow.format.ps1xml')

$DiagnosticSource = 'Get-SaveModDrift'

$descriptorPath = Join-Path $StarsectorRoot "saves\$SaveFolder\descriptor.xml"

if (-not (Test-Path -LiteralPath $descriptorPath -PathType Leaf)) {
    throw "descriptor not found: $descriptorPath"
}

$descriptor = Read-XmlDocument -Path $descriptorPath
$idIndex = New-XStreamIdIndex -Document $descriptor
$saveData = $descriptor.DocumentElement

$savedAt = Format-ConsoleText -Role 'Time' -Text (Get-XmlChildText -Element $saveData -Name 'saveDate')
$gameVersion = Format-ConsoleText -Role 'Mod' -Text (Get-XmlChildText -Element $saveData -Name 'gameVersion')

Write-DiagnosticLine -Source $DiagnosticSource -Message (
    "read $(Format-ConsoleText -Role 'Path' -Text $descriptorPath), saved $savedAt under Starsector $gameVersion; " +
    "$(Format-ConsoleNumber $idIndex.Count) objects carry an ID for back-pointers")

$savedMods = Read-SaveModList -SaveData $saveData -ListName 'enabledMods' -IdIndex $idIndex
$everEnabledMods = Read-SaveModList -SaveData $saveData -ListName 'allModsEverEnabled' -IdIndex $idIndex

# No list says nothing about the save's mods, where an empty list says it had none, so no drift is guessed.
if ($null -eq $savedMods) {

    Write-Warning "the descriptor lists no enabled mods, so no drift is reported: $descriptorPath"

    return
}

# A mod enabled once and dropped since can still have content in the save, so stderr names it.
# A foreach over a missing list runs no iteration, where a pipeline would pass one null through.
$savedIds = @(foreach ($mod in $savedMods) { $mod.Id })
$droppedIds = @(foreach ($mod in $everEnabledMods) { if ($mod.Id -cnotin $savedIds) { $mod.Id } })
$droppedSummary = "$(Format-ConsoleNumber $droppedIds.Count) more were enabled in an earlier save and are not now"

if ($droppedIds.Count -gt 0) {
    $droppedSummary += ": $(@($droppedIds | ForEach-Object { Format-ConsoleText -Role 'Mod' -Text $_ }) -join ', ')"
}

Write-DiagnosticLine -Source $DiagnosticSource -Message (
    "read $(Format-ConsoleNumber $savedMods.Count) enabled mods as the save last wrote them; $droppedSummary")

$roster = @(& (Join-Path $PSScriptRoot 'Get-ModRoster.ps1') -StarsectorRoot $StarsectorRoot)
$rows = @(Compare-SaveModRoster -SavedMods $savedMods -Roster $roster)

Write-DiagnosticLine -Source $DiagnosticSource -Message (
    "compared them with $(Format-ConsoleNumber $roster.Count) installed mods: $(Format-ModDriftCounts -Rows $rows)")

if ($AsJson) {
    ConvertTo-Json -InputObject $rows

} else {
    $rows
}
