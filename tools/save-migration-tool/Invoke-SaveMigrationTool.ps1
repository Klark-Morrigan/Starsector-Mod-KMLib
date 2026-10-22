#requires -Version 7.0
<#
.SYNOPSIS
    Diagnoses one save end to end: its newest load in the logs, and the mods it was last written with.

.DESCRIPTION
    Runs the other scripts beside this one in order, so a person gets one report from one command.
    With no -SaveFolder it diagnoses the save of the newest failed load.
    Stdout carries the report: text for a person, or the report object as JSON with -AsJson.
    What each script read and decided goes to stderr, as each script writes it.
    The README beside this script lists what each part of the report comes from.

.EXAMPLE
    Invoke-SaveMigrationTool.ps1 -StarsectorRoot <install>
    Diagnoses the save of the newest failed load.
#>

[CmdletBinding()]
param(
    # Starsector install root; the logs and saves are resolved beneath it.
    [Parameter(Mandatory)]
    [string] $StarsectorRoot,

    # The save's folder name under saves\. Without it, the save of the newest failed load.
    [string] $SaveFolder,

    # Print the report object as JSON instead of text, for a caller that parses the output.
    [switch] $AsJson
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\Import-SaveMigrationToolLibrary.ps1')

$DiagnosticSource = 'Invoke-SaveMigrationTool'

# Deep enough for a class owner in the load's object chain, one level below Get-SaveLoadAttempt's own output.
$JsonDepth = 7

# Only the one load the report shows is asked for, so class owners are resolved for that load alone.
$loadFilter = if ($SaveFolder) {
    @{ SaveFolder = $SaveFolder }
} else {
    @{ Outcome = (Get-LoadOutcomeNames).Failed }
}

$load = & (Join-Path $PSScriptRoot 'Get-SaveLoadAttempt.ps1') -StarsectorRoot $StarsectorRoot -Newest @loadFilter

if ($SaveFolder) {

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "diagnosing $(Format-ConsoleText -Role 'Path' -Text $SaveFolder), as asked")

} elseif ($null -ne $load) {

    $SaveFolder = $load.SaveFolder
    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "diagnosing the save of the newest failed load, $(Format-ConsoleText -Role 'Path' -Text $SaveFolder)")

} else {
    Write-DiagnosticLine -Source $DiagnosticSource -Message 'no load failed, so no save is diagnosed'
}

$mods = @()

if ($SaveFolder) {

    $modListScript = Join-Path $PSScriptRoot 'Get-SaveModDrift.ps1'
    $mods = @(& $modListScript -StarsectorRoot $StarsectorRoot -SaveFolder $SaveFolder)
}

$report = [pscustomobject]@{
    SaveFolder = $SaveFolder
    Load       = $load
    Mods       = $mods
}

if ($AsJson) {
    ConvertTo-Json -InputObject $report -Depth $JsonDepth
} else {
    Resolve-OutputLines -Lines (Format-DiagnosisReport -Report $report)
}
