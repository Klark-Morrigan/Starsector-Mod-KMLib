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

    # The log to read. Its rotated siblings are read with it.
    [Parameter(Mandatory, ParameterSetName = 'Log')]
    [string] $LogPath,

    # Print JSON instead of objects, for a caller that parses the output.
    [switch] $AsJson
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\class-index\Get-CoreOwnerName.ps1')
. (Join-Path $PSScriptRoot 'lib\class-index\New-ClassOwnerIndex.ps1')
. (Join-Path $PSScriptRoot 'lib\class-index\Read-CoreClasspath.ps1')
. (Join-Path $PSScriptRoot 'lib\class-index\Read-JarClassNames.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\Add-MissingContent.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\Add-ObjectChainOwner.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\ConvertFrom-LogLine.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\ConvertFrom-ThrowableHeader.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\ConvertFrom-XStreamClassLine.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\Format-MissingContentLine.ps1')
. (Join-Path $PSScriptRoot 'lib\load-log\Format-ObjectChainLine.ps1')
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

# The log sits in starsector-core, so the install it belongs to is the folder above.
$installRoot = Split-Path -Parent $saveBaseDirectory
$modsRoot = Join-Path $installRoot 'mods'

# Opening every jar takes seconds, so the index is built only when a chain needs it.
$ownersByClass = @{}

if (@($attempts | Where-Object { $_.ObjectChain.Count -gt 0 }).Count -eq 0) {
    Write-DiagnosticLine -Source $DiagnosticSource -Message 'no failure names an object chain; skipping the class index'

} elseif (-not (Test-Path -LiteralPath $modsRoot -PathType Container)) {
    Write-Warning "no mods folder in the log's install, $installRoot; object chain classes stay without owners"

} else {

    $roster = @(& (Join-Path $PSScriptRoot 'Get-ModRoster.ps1') -StarsectorRoot $installRoot)
    $coreJars = @(Read-CoreClasspath -StarsectorRoot $installRoot)

    if ($coreJars.Count -eq 0) {
        Write-Warning "vmparams in $installRoot names no classpath; vanilla classes stay without owners"
    }

    $sources = @([pscustomobject]@{ Owner = Get-CoreOwnerName; Jars = $coreJars }) +
        @($roster | ForEach-Object { [pscustomobject]@{ Owner = $_.Id; Jars = $_.Jars } })

    $index = New-ClassOwnerIndex -Sources $sources
    $ownersByClass = $index.OwnersByClass

    foreach ($missingJar in $index.MissingJars) {

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "skipped a declared jar that is not on disk: $missingJar")
    }

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "indexed classes $($ownersByClass.Count), jars $($index.JarCount), " +
        "from mods $($roster.Count) and core classpath entries $($coreJars.Count); " +
        "classes in more than one owner's jars $($index.SharedClassCount)")
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
    ConvertTo-Json -InputObject $loads -Depth 6

} else {
    $loads
}
