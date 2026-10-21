#requires -Version 7.0
<#
.SYNOPSIS
    Lists every save load in starsector.log and its rotated siblings, newest first:
    the save folder, the outcome, the cause of a failure, the missing content the cause names,
    and the chain of objects the failure sat in, each class with the mods whose jars hold it.

.DESCRIPTION
    Vanilla's own retries of a load fold into the load that caused them.
    The log sits in the install's starsector-core folder, and the jars that own each class are that install's.
    Stdout carries the loads only. What the script read and decided goes to stderr.
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

    # Print JSON instead of objects, for a caller that parses the output.
    [switch] $AsJson
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\Import-SaveMigrationToolLibrary.ps1')

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
Write-DiagnosticLine -Source $DiagnosticSource -Message "reading $($logFiles.Count) log files, oldest first"

$read = Read-LoadAttempt -LogFiles $logFiles -SaveBaseDirectory $saveBaseDirectory

foreach ($fileRead in $read.FileReads) {

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $($fileRead.LogFile): $($fileRead.LineCount) lines, $($fileRead.AttemptCount) load attempts start here")
}

$attempts = @($read.Attempts)
$chainClassNames = @($attempts | ForEach-Object { $_.ObjectChain } | ForEach-Object { $_.Class } | Select-Object -Unique)
$hasObjectChain = $chainClassNames.Count -gt 0
$ownersByClass = @{}

if (-not $hasObjectChain) {
    Write-DiagnosticLine -Source $DiagnosticSource -Message 'no failure names an object chain; resolving no class owners'

} elseif (-not (Test-Path -LiteralPath (Join-Path $installRoot 'mods') -PathType Container)) {
    Write-Warning "no mods folder in the log's install, $installRoot; object chain classes stay without owners"

} else {

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "resolving the owners of $($chainClassNames.Count) object chain classes from the install at $installRoot")

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $roster = @(& (Join-Path $PSScriptRoot 'Get-ModRoster.ps1') -StarsectorRoot $installRoot)
    $resolved = Resolve-ClassOwner -StarsectorRoot $installRoot -Roster $roster -ClassNames $chainClassNames
    $ownersByClass = $resolved.OwnersByClass

    if ($resolved.CoreJarCount -eq 0) {
        Write-Warning "vmparams in $installRoot names no classpath; vanilla classes stay without owners"
    }

    foreach ($skippedJar in $resolved.SkippedJars) {
        Write-DiagnosticLine -Source $DiagnosticSource -Message "skipped jar $($skippedJar.Path): $($skippedJar.Reason)"
    }

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $($resolved.JarCount) jars of $($roster.Count) mods and $($resolved.CoreJarCount) core classpath entries " +
        "in $($stopwatch.ElapsedMilliseconds) ms; owners found for $($ownersByClass.Count) classes, " +
        "more than one mod for $($resolved.SharedClassCount), " +
        "mod copies of core classes dropped for $($resolved.ShadowedClassCount)")
}

$loads = @(Group-LoadAttempt -Attempts $attempts | ForEach-Object {
    Add-ObjectChainOwner -Load (Add-MissingContent -Load $_) -OwnersByClass $ownersByClass
})
[array]::Reverse($loads)

$failedOutcome = (Get-LoadOutcomeNames).Failed
$failedCount = @($loads | Where-Object { $_.Outcome -eq $failedOutcome }).Count

Write-DiagnosticLine -Source $DiagnosticSource -Message (
    "grouped $($attempts.Count) attempts into $($loads.Count) loads, $failedCount failed; listing them newest first")

foreach ($load in $loads | Where-Object { $_.CauseType }) {

    Write-DiagnosticLine -Source $DiagnosticSource -Message (Format-MissingContentLine -Load $load)

    if ($load.ObjectChain.Count -gt 0) {
        Write-DiagnosticLine -Source $DiagnosticSource -Message (Format-ObjectChainLine -Load $load)
    }
}

if ($AsJson) {
    ConvertTo-Json -InputObject $loads -Depth $JsonDepth

} else {
    $loads
}
