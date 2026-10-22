#requires -Version 7.0
<#
.SYNOPSIS
    Lists the mods a save was last written with: each enabled mod's ID, name and version, from the save's descriptor.

.DESCRIPTION
    The descriptor is the small file beside campaign.xml that the game's load list reads.
    It holds the same mod list the load list's hover tooltip compares against the installed mods.
    Stdout carries the mods only. What the script read and decided goes to stderr.
    The README beside this script explains how the descriptor stores the list.

.EXAMPLE
    Get-SaveModDrift.ps1 -StarsectorRoot <install> -SaveFolder <save folder> -AsJson
    Lists the mods the save was last written with, as JSON.
#>

[CmdletBinding()]
param(
    # Starsector install root; the save is resolved beneath it.
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

$mods = Read-SaveModList -SaveData $saveData -ListName 'enabledMods' -IdIndex $idIndex
$everEnabledMods = Read-SaveModList -SaveData $saveData -ListName 'allModsEverEnabled' -IdIndex $idIndex

if ($null -eq $mods) {

    Write-Warning "the descriptor lists no enabled mods: $descriptorPath"
    $mods = @()
}

# A mod enabled once and dropped since can still have content in the save, so stderr names it.
# A foreach over a missing list runs no iteration, where a pipeline would pass one null through.
$enabledIds = @(foreach ($mod in $mods) { $mod.Id })
$droppedIds = @(foreach ($mod in $everEnabledMods) { if ($mod.Id -cnotin $enabledIds) { $mod.Id } })
$droppedSummary = "$(Format-ConsoleNumber $droppedIds.Count) more were enabled in an earlier save and are not now"

if ($droppedIds.Count -gt 0) {
    $droppedSummary += ": $(@($droppedIds | ForEach-Object { Format-ConsoleText -Role 'Mod' -Text $_ }) -join ', ')"
}

Write-DiagnosticLine -Source $DiagnosticSource -Message (
    "listing $(Format-ConsoleNumber $mods.Count) enabled mods as the save last wrote them; $droppedSummary")

if ($AsJson) {
    ConvertTo-Json -InputObject $mods

} else {
    $mods
}
