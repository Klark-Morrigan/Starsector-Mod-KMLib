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

$roster = @(Read-ModRoster -StarsectorRoot $StarsectorRoot -DiagnosticSource $DiagnosticSource)

$drift = Read-SaveModDrift `
    -StarsectorRoot $StarsectorRoot `
    -SaveFolder $SaveFolder `
    -Roster $roster `
    -DiagnosticSource $DiagnosticSource

if ($null -eq $drift.Rows) {

    Write-Warning "$($drift.Problem); no drift is reported"

    return
}

if ($AsJson) {
    ConvertTo-Json -InputObject $drift.Rows

} else {
    $drift.Rows
}
