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
$allLoads = @(Group-LoadAttempt -Attempts $attempts | ForEach-Object { Add-MissingContent -Load $_ })

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

$chainClassNames = @(
    $loads |
        ForEach-Object { $_.Attempts } |
        ForEach-Object { $_.ObjectChain } |
        ForEach-Object { $_.Class } |
        Select-Object -Unique
)

$hasObjectChain = $chainClassNames.Count -gt 0
$ownersByClass = @{}

if (-not $hasObjectChain) {
    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        'no listed failure names an object chain; resolving no class owners')

} elseif (-not (Test-Path -LiteralPath (Join-Path $installRoot 'mods') -PathType Container)) {
    Write-Warning "no mods folder in the log's install, $installRoot; object chain classes stay without owners"

} else {

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "resolving the owners of $(Format-ConsoleNumber $chainClassNames.Count) object chain classes " +
        "from the install at $(Format-ConsoleText -Role 'Path' -Text $installRoot)")

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $roster = @(& (Join-Path $PSScriptRoot 'Get-ModRoster.ps1') -StarsectorRoot $installRoot)
    $resolved = Resolve-ClassOwner -StarsectorRoot $installRoot -Roster $roster -ClassNames $chainClassNames
    $ownersByClass = $resolved.OwnersByClass

    if ($resolved.CoreJarCount -eq 0) {
        Write-Warning "vmparams in $installRoot names no classpath; vanilla classes stay without owners"
    }

    foreach ($skippedJar in $resolved.SkippedJars) {

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "skipped JAR $(Format-ConsoleText -Role 'Path' -Text $skippedJar.Path): $($skippedJar.Reason)")
    }

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $(Format-ConsoleNumber $resolved.JarCount) JARs of $(Format-ConsoleNumber $roster.Count) mods " +
        "and $(Format-ConsoleNumber $resolved.CoreJarCount) core classpath entries " +
        "in $(Format-ConsoleText -Role 'Time' -Text "$($stopwatch.ElapsedMilliseconds) ms"); " +
        "owners found for $(Format-ConsoleNumber $ownersByClass.Count) classes, " +
        "more than one mod for $(Format-ConsoleNumber $resolved.SharedClassCount), " +
        "mod copies of core classes dropped for $(Format-ConsoleNumber $resolved.ShadowedClassCount)")
}

$loads = @($loads | ForEach-Object { Add-ObjectChainOwner -Load $_ -OwnersByClass $ownersByClass })

# Each load's readings are a block of their own, parted from the lines around them and from the next load's.
foreach ($load in $loads | Where-Object { $_.CauseType }) {

    Write-DiagnosticLine -Source $DiagnosticSource -Message (Format-MissingContentLine -Load $load) -IsBlockStart

    if ($load.ObjectChain.Count -gt 0) {
        Write-DiagnosticLine -Source $DiagnosticSource -Message ((Format-ObjectChainLines -Load $load) -join "`n")
    }
}

if ($AsJson) {
    ConvertTo-Json -InputObject $loads -Depth $JsonDepth

} elseif ($AsText) {
    Format-LoadListText -Loads $loads | ForEach-Object { Resolve-ConsoleText -Text $_ -Stream 'Output' }

} else {
    $loads
}
