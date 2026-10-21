#requires -Version 7.0
<#
.SYNOPSIS
    Lists the installed mods the game loads: for each enabled mod id, the one folder that wins, at its version.

.DESCRIPTION
    Folder names are not a usable key: the same mod ships as both "bmo" and "Building Menu Overhaul-2.1.0".
    mod_info.json's "id" is the identity and "version" the recency; enabled_mods.json says which ids the game loads.
    Disabled mods and superseded copies stay on disk; the roster only leaves them out.

    Stdout carries the roster only. What the script read and decided goes to stderr, and warnings name each
    mod_info.json or enabled_mods.json it could not read.
    The README beside this script explains how each file is read.

.EXAMPLE
    Get-ModRoster.ps1 -StarsectorRoot <install>
    Lists the enabled mods of the install.
#>

[CmdletBinding()]
param(
    # Starsector install root. The mods directory is resolved beneath it.
    [Parameter(Mandatory)]
    [string] $StarsectorRoot,

    # Emit every installed mod rather than only those enabled_mods.json lists.
    [switch] $IncludeDisabled
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\Import-SaveMigrationToolLibrary.ps1')

$DiagnosticSource = 'Get-ModRoster'

$modsRoot = Join-Path $StarsectorRoot 'mods'

if (-not (Test-Path -LiteralPath $modsRoot -PathType Container)) {
    throw "mods directory not found: $modsRoot"
}

# Every path the roster reports is relative to this one, so it is resolved once, whatever form it was given in.
$installRoot = (Resolve-Path -LiteralPath $StarsectorRoot).Path

$enabled = Read-EnabledModIds -ModsRoot $modsRoot
$isEveryModListed = $IncludeDisabled -or -not $enabled.IsListRead
$enabledSet = [System.Collections.Generic.HashSet[string]]::new(
    [string[]] $enabled.Ids,
    [System.StringComparer]::OrdinalIgnoreCase)

$candidates = [System.Collections.Generic.List[object]]::new()
$skippedFolders = [System.Collections.Generic.List[string]]::new()

foreach ($folder in Get-ChildItem -LiteralPath $modsRoot -Directory) {

    $candidate = Read-ModCandidate -Folder $folder

    if ($null -eq $candidate) {
        $skippedFolders.Add($folder.Name)

    } else {
        $candidates.Add($candidate)
    }
}

if ($skippedFolders.Count -gt 0) {

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "skipped $($skippedFolders.Count) folders with no mod_info.json: $($skippedFolders -join ', ')")
}

$parsedStatus = (Get-ModStatusNames).Parsed
$roster = [System.Collections.Generic.List[object]]::new()
$supersededCount = 0
$disabledCount = 0

foreach ($modGroup in (Group-ModCandidate -Candidates $candidates.ToArray())) {

    $winner = $modGroup.Winner

    foreach ($loser in $modGroup.Superseded) {

        $supersededCount++
        $winnerCopy = "$($winner.Folder.Name) ($(Format-ModVersion -Version $winner.Version))"
        $loserCopy = "$($loser.Folder.Name) ($(Format-ModVersion -Version $loser.Version))"

        Write-DiagnosticLine -Source $DiagnosticSource -Message "$($modGroup.Id): $winnerCopy wins over $loserCopy"
    }

    # Unparsed entries bypass the enabled filter: their id is unknown, so the
    # only safe assumption is that they might matter.
    $isListed = $isEveryModListed -or
        $winner.Status -ne $parsedStatus -or
        $enabledSet.Contains($modGroup.Id)

    if (-not $isListed) {

        $disabledCount++
        continue
    }

    $roster.Add((ConvertTo-RosterEntry -ModGroup $modGroup -StarsectorRoot $installRoot))
}

$unparsedCount = @($roster | Where-Object { $_.Status -ne $parsedStatus }).Count

Write-DiagnosticLine -Source $DiagnosticSource -Message (
    "read $($candidates.Count) mod folders under $modsRoot and $($enabled.Ids.Count) enabled IDs; " +
    "listing $($roster.Count) mods ($unparsedCount unparsed), " +
    "skipping $disabledCount disabled and $supersededCount superseded copies")

$roster
