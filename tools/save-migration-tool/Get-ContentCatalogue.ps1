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

$ownersByKind = $catalogue.OwnersByKind
$idPatterns = @(foreach ($pattern in $Id) { [WildcardPattern]::new($pattern, 'IgnoreCase') })
$definedCount = 0

$rows = @(foreach ($kindName in $ownersByKind.Keys) {

    $ownersById = $ownersByKind[$kindName]
    $definedCount += $ownersById.Count

    $ids = [string[]] $ownersById.Keys
    [System.Array]::Sort($ids, [System.StringComparer]::Ordinal)

    foreach ($definedId in $ids) {

        $isListed = $idPatterns.Count -eq 0

        foreach ($pattern in $idPatterns) {

            if ($pattern.IsMatch($definedId)) {

                $isListed = $true
                break
            }
        }

        if ($isListed) {
            [pscustomobject]@{ Kind = $kindName; Id = $definedId; Owners = $ownersById[$definedId].ToArray() }
        }
    }
})

$listing = "listing $(Format-ConsoleNumber $rows.Count) of $(Format-ConsoleNumber $definedCount) defined IDs"

if ($GroupByOwner) {

    $rows = @(Group-ContentByOwner -Rows $rows -Sources $catalogue.Sources)
    $listing += " under $(Format-ConsoleNumber $rows.Count) owners"
}

Write-DiagnosticLine -Source $DiagnosticSource -Message $listing

if ($OutFile) {

    # .NET resolves a relative path against the process's folder, not the shell's.
    $outPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutFile)

    New-Item -ItemType Directory -Path (Split-Path -Parent $outPath) -Force | Out-Null
    [System.IO.File]::WriteAllText(
        $outPath,
        (ConvertTo-Json -InputObject $rows -Depth 4),
        [System.Text.UTF8Encoding]::new($false))

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "wrote the JSON to $(Format-ConsoleText -Role 'Path' -Text $outPath)")

} elseif ($AsJson) {
    ConvertTo-Json -InputObject $rows -Depth 4

} else {
    $rows
}
