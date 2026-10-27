#requires -Version 7.0
<#
.SYNOPSIS
    Lists the installed mods the game loads: for each enabled mod ID, the one folder that wins, at its version.

.DESCRIPTION
    Stdout carries the roster only. What the script read and decided goes to stderr, and warnings name each
    mod_info.json or enabled_mods.json it could not read.
    The README beside this script explains how each file is read.

.EXAMPLE
    Get-ModRoster.ps1 -StarsectorRoot <install>
    Lists the enabled mods of the install.
#>

[CmdletBinding()]
param(
    # Starsector install root. The mods directory is resolved beneath it.
    [Parameter(Mandatory)]
    [string] $StarsectorRoot,

    # Emit every installed mod rather than only those enabled_mods.json lists.
    [switch] $IncludeDisabled
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\Import-SaveMigrationToolLibrary.ps1')

Read-ModRoster -StarsectorRoot $StarsectorRoot -IncludeDisabled:$IncludeDisabled -DiagnosticSource 'Get-ModRoster'
