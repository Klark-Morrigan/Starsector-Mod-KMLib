#requires -Version 7.0
<#
.SYNOPSIS
    Diagnoses one save end to end: its newest load in the logs, and how its mods differ from the installed ones.

.DESCRIPTION
    Reads the installed mods once, then runs Get-SaveLoadAttempt.ps1 and compares the save's mods,
    so a person gets one report from one command.
    With no -SaveFolder it diagnoses the save of the newest failed load.
    Stdout carries the report: text for a person, or the report object as JSON with -AsJson.
    What each part read and decided goes to stderr.
    The README beside this script lists what each part of the report comes from.

.EXAMPLE
    Invoke-SaveMigrationTool.ps1 -StarsectorRoot <install>
    Diagnoses the save of the newest failed load.
#>

[CmdletBinding()]
param(
    # Starsector install root; the logs, saves and mods are resolved beneath it.
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

# Both parts need the installed mods: the load for its class owners, the drift for its installed side.
$roster = @(Read-ModRoster -StarsectorRoot $StarsectorRoot -DiagnosticSource $DiagnosticSource)

# Only the one load the report shows is asked for, so class owners are resolved for that load alone.
$loadFilter = if ($SaveFolder) {
    @{ SaveFolder = $SaveFolder }
} else {
    @{ Outcome = (Get-LoadOutcomeNames).Failed }
}

$loadScript = Join-Path $PSScriptRoot 'Get-SaveLoadAttempt.ps1'
$load = & $loadScript -StarsectorRoot $StarsectorRoot -Newest -Roster $roster @loadFilter

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

$modDrift = $null
$modDriftProblem = $null

if ($SaveFolder) {

    $descriptorPath = Get-SaveDescriptorPath -StarsectorRoot $StarsectorRoot -SaveFolder $SaveFolder

    # A save deleted or renamed since its load still has a load worth reporting.
    if (Test-Path -LiteralPath $descriptorPath -PathType Leaf) {

        $drift = Read-SaveModDrift -DescriptorPath $descriptorPath -Roster $roster -DiagnosticSource $DiagnosticSource
        $modDrift = $drift.Rows
        $modDriftProblem = $drift.Problem

    } else {
        $modDriftProblem = "descriptor not found: $descriptorPath"
    }

    if ($modDriftProblem) {
        Write-DiagnosticLine -Source $DiagnosticSource -Message "comparing no mods: $modDriftProblem"
    }
}

$report = [pscustomobject]@{
    SaveFolder      = $SaveFolder
    Load            = $load
    ModDrift        = $modDrift
    ModDriftProblem = $modDriftProblem
}

$driftSummary = if ($null -ne $modDrift) {
    "$(Format-ConsoleNumber $modDrift.Count) mod drift rows"
} else {
    'no mod drift'
}

if ($AsJson) {

    Write-DiagnosticLine -Source $DiagnosticSource -Message "writing the report as JSON, with $driftSummary"

    # The report holds the load one level below Get-SaveLoadAttempt.ps1's own output.
    ConvertTo-Json -InputObject $report -Depth ((Get-LoadJsonDepth) + 1)

} else {

    Write-DiagnosticLine -Source $DiagnosticSource -Message "writing the report as text, with $driftSummary"
    Resolve-OutputLines -Lines (Format-DiagnosisReport -Report $report)
}
