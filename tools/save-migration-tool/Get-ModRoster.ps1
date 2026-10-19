#requires -Version 5.1
# Resolves the "live" Starsector mod roster: for each mod id the game will
# actually load, the single installed folder that wins.
#
# Folder names are not a usable key - the same mod ships as both "bmo" and
# "Building Menu Overhaul-2.1.0". mod_info.json's "id" is the identity and
# "version" the recency; enabled_mods.json says which ids the game loads.
# A tool scoped to this roster leaves disabled mods and superseded copies out
# without deleting anything from disk, so an older version stays reachable by
# naming its folder explicitly.
#
# Fails OPEN: a mod_info.json that cannot be parsed still yields a roster
# entry (Status 'unparsed') plus a warning, never a silent drop. A filter
# that hides a real mod is worse than no filter at all, because nothing
# signals that it happened.
#
# Stdout carries the roster only. The summary of what was read and decided
# goes to stderr.

[CmdletBinding()]
param(
    # Starsector install root. The mods directory is resolved beneath it.
    [string] $StarsectorRoot = 'c:\a_Games\Starsector',

    # Emit every installed mod rather than only those enabled_mods.json lists.
    [switch] $IncludeDisabled
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\ConvertFrom-StarsectorJson.ps1')
. (Join-Path $PSScriptRoot 'lib\Format-ModVersion.ps1')
. (Join-Path $PSScriptRoot 'lib\Get-JsonPropertyValue.ps1')
. (Join-Path $PSScriptRoot 'lib\Get-VersionObjectParts.ps1')
. (Join-Path $PSScriptRoot 'lib\Get-VersionSortKey.ps1')
. (Join-Path $PSScriptRoot 'lib\Remove-JsonComment.ps1')
. (Join-Path $PSScriptRoot 'lib\Write-DiagnosticLine.ps1')

$DiagnosticSource = 'Get-ModRoster'

$StatusParsed = 'ok'
$StatusUnparsed = 'unparsed'

$modsRoot = Join-Path $StarsectorRoot 'mods'

if (-not (Test-Path $modsRoot)) {
    throw "mods directory not found: $modsRoot"
}

# An unreadable enabled_mods.json must not empty the roster, so fall back to
# treating every installed mod as live.
$enabledIds = @()
$enabledFile = Join-Path $modsRoot 'enabled_mods.json'

if (Test-Path $enabledFile) {

    try {

        # Read through the property, not Get-JsonPropertyValue: returning an empty list from a function unrolls it
        # to null, and an install with no mod enabled holds exactly that.
        $enabledProperty = (ConvertFrom-StarsectorJson -Path $enabledFile).PSObject.Properties['enabledMods']

        if ($null -eq $enabledProperty) {
            throw 'it holds no enabledMods list'
        }

        $enabledIds = @($enabledProperty.Value | Where-Object { $_ })

    } catch {

        Write-Warning "enabled_mods.json unparseable, treating all mods as enabled: $($_.Exception.Message)"
        $IncludeDisabled = $true
    }

} else {

    Write-Warning "enabled_mods.json not found, treating all mods as enabled."
    $IncludeDisabled = $true
}

$enabledSet = [System.Collections.Generic.HashSet[string]]::new(
    [string[]] $enabledIds,
    [System.StringComparer]::OrdinalIgnoreCase)

$candidates = [System.Collections.Generic.List[object]]::new()

foreach ($dir in Get-ChildItem -Path $modsRoot -Directory) {

    $infoPath = Join-Path $dir.FullName 'mod_info.json'

    if (-not (Test-Path $infoPath)) {
        continue
    }

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

        Write-Warning "Unparseable mod_info.json, keeping in roster: $($dir.Name) ($reason)"

        $candidates.Add([pscustomobject]@{
            Id      = $dir.Name
            Version = $null
            SortKey = ''
            Folder  = $dir
            Status  = $StatusUnparsed
        })

        continue
    }

    $version = Get-JsonPropertyValue -Object $info -Name 'version'

    $candidates.Add([pscustomobject]@{
        Id      = [string] $modId
        Version = $version
        SortKey = Get-VersionSortKey -Version $version
        Folder  = $dir
        Status  = $StatusParsed
    })
}

$roster = [System.Collections.Generic.List[object]]::new()
$supersededCount = 0
$disabledCount = 0

foreach ($group in ($candidates | Group-Object -Property Id)) {

    $ordered = $group.Group | Sort-Object -Property SortKey -Descending
    $winner = $ordered | Select-Object -First 1
    $supersededCount += $group.Count - 1

    # Unparsed entries bypass the enabled filter: their id is unknown, so the
    # only safe assumption is that they might matter.
    if ($winner.Status -eq $StatusParsed -and -not $IncludeDisabled -and -not $enabledSet.Contains($winner.Id)) {

        $disabledCount++
        continue
    }

    $roster.Add([pscustomobject]@{
        Id              = $winner.Id
        Version         = Format-ModVersion -Version $winner.Version
        Folder          = $winner.Folder.FullName
        RelativePath    = $winner.Folder.FullName.Substring($StarsectorRoot.Length).TrimStart('\')
        Status          = $winner.Status
        SupersededCount = $group.Count - 1
    })
}

$unparsedCount = @($roster | Where-Object { $_.Status -eq $StatusUnparsed }).Count

Write-DiagnosticLine -Source $DiagnosticSource -Message (
    "read $($candidates.Count) mod folders under $modsRoot and $($enabledIds.Count) enabled IDs; " +
    "listing $($roster.Count) mods ($unparsedCount unparsed), " +
    "skipping $disabledCount disabled and $supersededCount superseded copies")

$roster
