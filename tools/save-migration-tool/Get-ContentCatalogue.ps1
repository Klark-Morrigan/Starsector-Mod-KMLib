#requires -Version 7.0
<#
.SYNOPSIS
    Lists the content IDs the install defines, per kind, each with the mods that define it.

.DESCRIPTION
    Reads the game's own data and every enabled mod's as the game does at startup:
    hull, skin, weapon and variant files, and the spreadsheets of hulls, weapons, fighter wings, hullmods,
    market conditions, industries, special items, commodities, abilities and submarkets.
    It adds the d-hulls and empty hull variants the game builds from them.
    An ID a save names and this list lacks is content no installed mod provides any more.
    Stdout carries one row per defined ID, or one entry per owner with -GroupByOwner.
    What the script read and decided goes to stderr.
    The README beside this script explains which files define each kind.

.EXAMPLE
    Get-ContentCatalogue.ps1 -StarsectorRoot <install> -Id 'ssp_*'
    Lists every defined ID starting with ssp_, of every kind, with the mods that define it.

.EXAMPLE
    Get-ContentCatalogue.ps1 -StarsectorRoot <install> -GroupByOwner -OutFile <file>
    Writes every defined ID as JSON to a file, grouped by the mod that defines it.
#>

[CmdletBinding()]
param(
    # Starsector install root; the game's data and the mods are resolved beneath it.
    [Parameter(Mandatory)]
    [string] $StarsectorRoot,

    # The kinds to list; every kind when left out. Only their files are read.
    [string[]] $Kind,

    # Wildcard patterns an ID must match one of, ignoring case. Every ID when left out.
    [string[]] $Id,

    # One entry per owner, holding its IDs by kind, instead of one row per ID.
    [switch] $GroupByOwner,

    # Print JSON instead of objects, for a caller that parses the output.
    [switch] $AsJson,

    # Write the JSON to this file instead of stdout. A full list outruns a console's scrollback.
    [string] $OutFile,

    # The install's enabled mods, as Get-ModRoster.ps1 lists them, from a caller that has read them already.
    [object[]] $Roster
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\Import-SaveMigrationToolLibrary.ps1')

$DiagnosticSource = 'Get-ContentCatalogue'

$cataloguedKinds = @((Get-ContentDefinitions).Kind)

if (-not $PSBoundParameters.ContainsKey('Kind')) {
    $Kind = $cataloguedKinds
}

$unknownKinds = @($Kind | Where-Object { $_ -cnotin $cataloguedKinds })

# A mistyped kind would read every file of the others and then list nothing of it.
if ($unknownKinds.Count -gt 0) {
    throw "unknown kind: $($unknownKinds -join ', '); the kinds are $($cataloguedKinds -join ', ')"
}

if (-not $PSBoundParameters.ContainsKey('Roster')) {
    $Roster = @(Read-ModRoster -StarsectorRoot $StarsectorRoot -DiagnosticSource $DiagnosticSource)
}

$catalogue = Read-ContentCatalogue -StarsectorRoot $StarsectorRoot -Roster $Roster -Kinds $Kind `
    -DiagnosticSource $DiagnosticSource

$definedCount = ($catalogue.OwnersByKind.Values | ForEach-Object Count | Measure-Object -Sum).Sum
$rows = @(ConvertTo-CatalogueRows -OwnersByKind $catalogue.OwnersByKind -IdPatterns $Id)

$listing = "listing $(Format-ConsoleNumber $rows.Count) of $(Format-ConsoleNumber $definedCount) defined IDs"

if ($GroupByOwner) {

    $rows = @(Group-ContentByOwner -Rows $rows -Sources $catalogue.Sources)
    $listing += " under $(Format-ConsoleNumber $rows.Count) owners"
}

Write-DiagnosticLine -Source $DiagnosticSource -Message $listing

if (-not $OutFile -and -not $AsJson) {
    $rows

} else {

    $json = ConvertTo-Json -InputObject $rows -Depth (Get-CatalogueJsonDepth)

    if ($OutFile) {

        $outPath = Write-TextFile -Path $OutFile -Text $json

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "wrote the JSON to $(Format-ConsoleText -Role 'Path' -Text $outPath)")

    } else {
        $json
    }
}
