#requires -Version 7.0
<#
.SYNOPSIS
    Lists every save load in starsector.log and its rotated siblings, newest first:
    the save folder, the outcome, the cause of a failure, the missing content the cause names,
    and the chain of objects the failure sat in, each class with the mods whose JARs hold it.

.DESCRIPTION
    Vanilla's own retries of a load fold into the load that caused them.
    The log sits in the install's starsector-core folder, and the JARs that own each class are that install's.
    Stdout carries the loads only: objects, JSON with -AsJson, or text for a person with -AsText.
    What the script read and decided goes to stderr.
    The filters apply before class owners are resolved, so asking for one load reads only the JARs it needs.
    The README beside this script explains how vanilla logs a load and what each field holds.

.EXAMPLE
    Get-SaveLoadAttempt.ps1 -StarsectorRoot <install> -AsJson
    Lists every load in the install's logs, as JSON.
#>

[CmdletBinding(DefaultParameterSetName = 'Install')]
param(
    # Starsector install root; the log is resolved beneath it.
    [Parameter(Mandatory, ParameterSetName = 'Install')]
    [string] $StarsectorRoot,

    # The log to read. Its rotated siblings are read with it, and class owners come from the install above its folder.
    [Parameter(Mandatory, ParameterSetName = 'Log')]
    [string] $LogPath,

    # Only the loads of this save, named by its folder under saves\.
    [string] $SaveFolder,

    # Only the loads that ended so: Loaded, Failed or Unfinished.
    [string] $Outcome,

    # Only the newest load left after the other filters.
    [switch] $Newest,

    # Print JSON instead of objects, for a caller that parses the output.
    [switch] $AsJson,

    # Print text for a person instead of objects: each load under its save, with its retries and mod classes.
    [switch] $AsText
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($AsJson -and $AsText) {
    throw '-AsJson and -AsText each choose the output; pass one'
}

. (Join-Path $PSScriptRoot 'lib\Import-SaveMigrationToolLibrary.ps1')

if ($Outcome) {
    Assert-LoadOutcomeName -Outcome $Outcome
}

$DiagnosticSource = 'Get-SaveLoadAttempt'

# The game's log location relative to the install root.
$DefaultLogRelativePath = 'starsector-core\starsector.log'

# Deep enough for each class owner: a load, its attempts, an attempt's object chain, and a class's owners.
$JsonDepth = 6

if ($PSCmdlet.ParameterSetName -eq 'Install') {
    $LogPath = Join-Path $StarsectorRoot $DefaultLogRelativePath
}

if (-not (Test-Path -LiteralPath $LogPath -PathType Leaf)) {
    throw "log not found: $LogPath"
}

$LogPath = (Resolve-Path -LiteralPath $LogPath).Path

# The game logs save paths as it opened them, relative to its working directory: the log's own folder.
$saveBaseDirectory = Split-Path -Parent $LogPath

# The log sits in starsector-core, so the install it belongs to is the folder above.
$installRoot = Split-Path -Parent $saveBaseDirectory

$logFiles = Get-LogFileChain -Path $LogPath
Write-DiagnosticLine -Source $DiagnosticSource -Message (
    "reading $(Format-ConsoleNumber $logFiles.Count) log files, oldest first")

$read = Read-LoadAttempt -LogFiles $logFiles -SaveBaseDirectory $saveBaseDirectory

foreach ($fileRead in $read.FileReads) {

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $(Format-ConsoleText -Role 'Path' -Text $fileRead.LogFile): " +
        "$(Format-ConsoleNumber $fileRead.LineCount) lines, " +
        "$(Format-ConsoleNumber $fileRead.AttemptCount) load attempts start here")
}

$attempts = @($read.Attempts)
$allLoads = @(Group-LoadAttempt -Attempts $attempts)

[array]::Reverse($allLoads)

$loads = Select-SaveLoad -Loads $allLoads -SaveFolder $SaveFolder -Outcome $Outcome -IsNewestOnly:$Newest
$failedOutcome = (Get-LoadOutcomeNames).Failed
$failedCount = @($allLoads | Where-Object { $_.Outcome -eq $failedOutcome }).Count

$filters = @(
    if ($SaveFolder) { "of save $(Format-ConsoleText -Role 'Path' -Text $SaveFolder)" }
    if ($Outcome) { "ending $Outcome" }
    if ($Newest) { 'the newest only' }
)

$keptSummary = if ($filters.Count -gt 0) {
    "listing $(Format-ConsoleNumber $loads.Count) newest first: $($filters -join ', ')"
} else {
    'listing them newest first'
}

Write-DiagnosticLine -Source $DiagnosticSource -Message (
    "grouped $(Format-ConsoleNumber $attempts.Count) attempts into $(Format-ConsoleNumber $allLoads.Count) loads, " +
    "$(Format-ConsoleNumber $failedCount) failed; $keptSummary")

# Traces are read only for the loads kept, since reading them is the slow part of a load's readings.
$loads = @($loads | ForEach-Object { Add-FailureReading -Load $_ -TraceLinesByAttempt $read.TraceLinesByAttempt })

$chainClassNames = @(
    $loads |
        ForEach-Object { $_.Attempts } |
        ForEach-Object { $_.ObjectChain } |
        ForEach-Object { $_.Class } |
        Select-Object -Unique
)

$ownerResolution = @{
    InstallRoot      = $installRoot
    ClassNames       = $chainClassNames
    RosterScriptPath = Join-Path $PSScriptRoot 'Get-ModRoster.ps1'
    DiagnosticSource = $DiagnosticSource
}

$ownersByClass = Resolve-ChainClassOwners @ownerResolution

$loads = @($loads | ForEach-Object {
    Copy-FirstAttemptReading -Load (Add-ObjectChainOwner -Load $_ -OwnersByClass $ownersByClass)
})

# Each load's readings are a block of their own, parted from the lines around them and from the next load's.
foreach ($load in $loads | Where-Object { $_.CauseType }) {

    $save = Format-ConsoleText -Role 'Path' -Text $load.SaveFolder
    $missingReading = "${save}: $(Format-MissingContentLine -Load $load)"

    Write-DiagnosticLine -Source $DiagnosticSource -Message $missingReading -IsBlockStart

    if ($load.ObjectChain.Count -gt 0) {
        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "${save}: $((Format-ObjectChainLines -Load $load) -join "`n")")
    }
}

if ($AsJson) {
    ConvertTo-Json -InputObject $loads -Depth $JsonDepth

} elseif ($AsText) {
    Resolve-OutputLines -Lines (Format-LoadListText -Loads $loads)

} else {
    $loads
}
