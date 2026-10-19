#requires -Version 7.0
<#
.SYNOPSIS
    Lists every save load in starsector.log, newest first: the save folder, the outcome, and the cause of a failure.

.DESCRIPTION
    A failed load buries its cause in a stack trace a thousand lines long.
    Vanilla then retries the load twice: from campaign.xml.bak, then from campaign.xml.inprogress.
    One click on Load can so leave three failures in the log,
    the last usually a FileNotFoundException for an .inprogress file that never existed.

    This script groups those retries back into the one load the player made.
    The load's cause is the one its first attempt hit, since campaign.xml is the newest file.
    Each attempt keeps its own cause and log position for a closer look.

    A cause usually names one id the save holds and no installed mod defines.
    Each cause is read into that content's kind and id, so the next step knows what to look for.

    Rotated logs (starsector.log.1, .2, ...) are read too, oldest first,
    so a load the game logged before a rotation is still found.
    Rotation renumbers the files, so a reported log position holds only until the game writes the next one.

    Loads are listed newest first.
    An older load is likelier to describe a save or a mod set that has changed since.

    Stdout carries the loads only. What the script read and decided goes to stderr.

.EXAMPLE
    Get-SaveLoadFailure.ps1 -AsJson
    Lists every load in the default install's logs, as JSON.
#>

[CmdletBinding()]
param(
    # Starsector install root. The log is resolved beneath it unless -LogPath names one.
    [string] $StarsectorRoot = 'c:\a_Games\Starsector',

    # The log to read. Its rotated siblings are read with it.
    [string] $LogPath,

    # Print JSON instead of objects, for a caller that parses the output.
    [switch] $AsJson
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\Format-MissingContentLine.ps1')
. (Join-Path $PSScriptRoot 'lib\Get-LoadOutcomeNames.ps1')
. (Join-Path $PSScriptRoot 'lib\Get-LogFileChain.ps1')
. (Join-Path $PSScriptRoot 'lib\Get-MissingContent.ps1')
. (Join-Path $PSScriptRoot 'lib\Group-LoadAttempt.ps1')
. (Join-Path $PSScriptRoot 'lib\Read-LoadAttempt.ps1')
. (Join-Path $PSScriptRoot 'lib\Write-DiagnosticLine.ps1')

$DiagnosticSource = 'Get-SaveLoadFailure'

# The game's log location relative to the install root.
$DefaultLogRelativePath = 'starsector-core\starsector.log'

if (-not $LogPath) {
    $LogPath = Join-Path $StarsectorRoot $DefaultLogRelativePath
}

if (-not (Test-Path -LiteralPath $LogPath -PathType Leaf)) {
    throw "log not found: $LogPath"
}

$LogPath = (Resolve-Path -LiteralPath $LogPath).Path

# The game logs save paths as it opened them, relative to its working directory: the log's own folder.
$saveBaseDirectory = Split-Path -Parent $LogPath

$logFiles = Get-LogFileChain -Path $LogPath
Write-DiagnosticLine -Source $DiagnosticSource -Message "reading $($logFiles.Count) log files, oldest first"

$read = Read-LoadAttempt -LogFiles $logFiles -SaveBaseDirectory $saveBaseDirectory

foreach ($fileRead in $read.FileReads) {

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $($fileRead.LogFile): $($fileRead.LineCount) lines, $($fileRead.AttemptCount) load attempts start here")
}

$attempts = @($read.Attempts)
$loads = @(Group-LoadAttempt -Attempts $attempts)
[array]::Reverse($loads)

$failedOutcome = (Get-LoadOutcomeNames).Failed
$failedCount = @($loads | Where-Object { $_.Outcome -eq $failedOutcome }).Count

Write-DiagnosticLine -Source $DiagnosticSource -Message (
    "grouped $($attempts.Count) attempts into $($loads.Count) loads, $failedCount failed; listing them newest first")

foreach ($load in $loads | Where-Object { $_.CauseType }) {
    Write-DiagnosticLine -Source $DiagnosticSource -Message (Format-MissingContentLine -Load $load)
}

if ($AsJson) {
    ConvertTo-Json -InputObject $loads -Depth 4

} else {
    $loads
}
