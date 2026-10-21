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

$loads = @(& (Join-Path $PSScriptRoot 'Get-SaveLoadAttempt.ps1') -StarsectorRoot $StarsectorRoot)

if ($SaveFolder) {

    $load = $loads | Where-Object { $_.SaveFolder -eq $SaveFolder } | Select-Object -First 1
    Write-DiagnosticLine -Source $DiagnosticSource -Message "diagnosing $SaveFolder, as asked"

} else {

    $failedOutcome = (Get-LoadOutcomeNames).Failed
    $load = $loads | Where-Object { $_.Outcome -eq $failedOutcome } | Select-Object -First 1
    $chosenSave = 'no save, since none failed'

    if ($null -ne $load) {

        $SaveFolder = $load.SaveFolder
        $chosenSave = $SaveFolder
    }

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "of $($loads.Count) loads, diagnosing the newest failed: $chosenSave")
}

$mods = @()

if ($SaveFolder) {
    $mods = @(& (Join-Path $PSScriptRoot 'Get-SaveModDrift.ps1') -StarsectorRoot $StarsectorRoot -SaveFolder $SaveFolder)
}

$report = [pscustomobject]@{
    SaveFolder = $SaveFolder
    Load       = $load
    Mods       = $mods
}

if ($AsJson) {
    ConvertTo-Json -InputObject $report -Depth $JsonDepth

} else {
    Format-DiagnosisReport -Report $report
}
