#requires -Version 7.0
<#
.SYNOPSIS
    Lists the content IDs a save names that no installed mod defines any more, with how often the save names each.

.DESCRIPTION
    Reads the save's campaign.xml once, at every site where the game's own classes write a content ID:
    hulls, weapons, fighter wings and hullmods on ships, in cargo and in the factions' known lists,
    and the market conditions, industries, special items, commodities, submarkets, factions, skills, personalities,
    abilities, custom entities, planets and terrain the campaign names.
    Each ID is looked up in the catalogue Get-ContentCatalogue.ps1 builds, read only for those kinds,
    and kept in the cache that script keeps.
    Stdout carries one row per missing ID. What the script read and decided goes to stderr.
    The README beside this script lists the sites it reads.

.EXAMPLE
    Find-SaveBrokenReferences.ps1 -StarsectorRoot <install> -SaveFolder <save folder>
    Lists the IDs the save names that the installed mods no longer define.
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
    [switch] $AsJson,

    # Read every catalogued kind from the files, whatever the cache holds, and cache it afresh.
    [switch] $Rebuild,

    # The install's enabled mods, as Get-ModRoster.ps1 lists them, from a caller that has read them already.
    [object[]] $Roster
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\Import-SaveMigrationToolLibrary.ps1')

$DiagnosticSource = 'Find-SaveBrokenReferences'

$campaignPath = Get-SaveFilePath `
    -StarsectorRoot $StarsectorRoot `
    -SaveFolder $SaveFolder `
    -FileName (Get-InstallPathNames).CampaignFile

# A missing save fails before the catalogue, which can take a minute to read.
if (-not [System.IO.File]::Exists($campaignPath)) {
    throw "save not found: $campaignPath"
}

if (-not $PSBoundParameters.ContainsKey('Roster')) {

    $Roster = @(Read-ModRoster `
        -StarsectorRoot $StarsectorRoot `
        -DiagnosticSource $DiagnosticSource)
}

$sites = @(Get-SaveReferenceSites)
$siteKinds = @($sites.Kind | Select-Object -Unique)

$catalogue = Read-ContentCatalogue `
    -StarsectorRoot $StarsectorRoot `
    -Roster $Roster `
    -Kinds $siteKinds `
    -IsRebuildForced:$Rebuild `
    -DiagnosticSource $DiagnosticSource

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

$scan = Read-SaveReferences `
    -Path $campaignPath `
    -Sites $sites

$useCount = 0

foreach ($reference in $scan.References) {
    $useCount += $reference.UseCount
}

Write-DiagnosticLine `
    -Source $DiagnosticSource `
    -Message (
        "read $(Format-ConsoleNumber $scan.ElementCount) elements of $(Format-ConsoleText -Role 'Path' -Text $campaignPath) " +
        "in $(Format-ConsoleText -Role 'Time' -Text "$($stopwatch.ElapsedMilliseconds) ms"): " +
        "$(Format-ConsoleNumber $scan.References.Count) IDs named $(Format-ConsoleNumber $useCount) times " +
        "at $(Format-ConsoleNumber $sites.Count) sites")

foreach ($unreadable in $scan.
        UnreadableCountBySite.
        GetEnumerator()) {

    Write-Warning ("$($unreadable.Value) values at $($unreadable.Key) are not the JSON object the game writes there; " +
        'their IDs are not counted')
}

$rows = @(Select-BrokenReferences `
    -References $scan.References `
    -OwnersByKind $catalogue.OwnersByKind)

$brokenUseCount = 0

# The rows' own order, which Group-Object would sort by name.
$brokenCountByKind = [ordered]@{}

foreach ($row in $rows) {

    $brokenUseCount += $row.UseCount
    $brokenCountByKind[$row.Kind] = 1 + [int] $brokenCountByKind[$row.Kind]
}

$kindCounts = foreach ($kind in $brokenCountByKind.Keys) {
    "$(Format-ConsoleNumber $brokenCountByKind[$kind]) $kind"
}

$brokenSummary = if ($rows.Count -eq 0) {
    'every ID the save names is defined'
} else {
    "$(Format-ConsoleNumber $rows.Count) IDs are not defined, named $(Format-ConsoleNumber $brokenUseCount) times: " +
        ($kindCounts -join ', ')
}

Write-DiagnosticLine `
    -Source $DiagnosticSource `
    -Message $brokenSummary

if ($AsJson) {
    ConvertTo-Json -InputObject $rows

} else {
    $rows
}
