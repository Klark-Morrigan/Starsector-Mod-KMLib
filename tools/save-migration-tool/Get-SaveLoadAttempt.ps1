#requires -Version 7.0
<#
.SYNOPSIS
    Lists every save load in starsector.log and its rotated siblings, newest first:
    the save folder, the outcome, the cause of a failure, and the missing content the cause names.

.DESCRIPTION
    Vanilla's own retries of a load fold into the load that caused them.
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

    # The log to read. Its rotated siblings are read with it.
    [Parameter(Mandatory, ParameterSetName = 'Log')]
    [string] $LogPath,

    # Print JSON instead of objects, for a caller that parses the output.
    [switch] $AsJson
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\load-log\Add-MissingContent.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\ConvertFrom-LogLine.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\ConvertFrom-ThrowableHeader.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\Format-MissingContentLine.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\Get-LoadOutcomeNames.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\Get-LogFileChain.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\Get-MissingContent.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\Group-LoadAttempt.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\Read-LoadAttempt.ps1')
. (Join-Path $PSScriptRoot 'lib\Write-DiagnosticLine.ps1')

$DiagnosticSource = 'Get-SaveLoadAttempt'

# The game's log location relative to the install root.
$DefaultLogRelativePath = 'starsector-core\starsector.log'

if ($PSCmdlet.ParameterSetName -eq 'Install') {
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
$loads = @(Group-LoadAttempt -Attempts $attempts | ForEach-Object { Add-MissingContent -Load $_ })
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
