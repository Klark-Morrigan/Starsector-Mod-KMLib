#requires -Version 5.1
# Resolves the "live" Starsector mod roster: for each mod id the game will
# actually load, the single installed folder that wins.
#
# Folder names are not a usable key - the same mod ships as both "bmo" and
# "Building Menu Overhaul-2.1.0". mod_info.json's "id" is the identity and
# "version" the recency; enabled_mods.json says which ids the game loads.
# A tool scoped to this roster leaves disabled mods and superseded copies out
# without deleting anything from disk, so an older version stays reachable by
# naming its folder explicitly.
#
# Fails OPEN: a mod_info.json that cannot be parsed still yields a roster
# entry (Status 'unparsed') plus a warning, never a silent drop. A filter
# that hides a real mod is worse than no filter at all, because nothing
# signals that it happened.
#
# Stdout carries the roster only. The summary of what was read and decided
# goes to stderr.

[CmdletBinding()]
param(
    # Starsector install root. The mods directory is resolved beneath it.
    [string] $StarsectorRoot = 'c:\a_Games\Starsector',

    # Emit every installed mod rather than only those enabled_mods.json lists.
    [switch] $IncludeDisabled
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Version components are zero-padded to this width so an ordinal string
# comparison matches numeric ordering ("10" must sort above "9").
$VersionPadWidth = 6

# Components compared per version. Starsector versions are major.minor.patch
# with an occasional build suffix, so four covers every observed shape.
$VersionPartCount = 4

$StatusParsed = 'ok'
$StatusUnparsed = 'unparsed'

function Write-DiagnosticLine {
    # Stderr keeps stdout for the roster, which callers consume as objects.
    param([string] $Message)

    [Console]::Error.WriteLine("Get-ModRoster: $Message")
}

function Get-JsonPropertyValue {
    <#
        Reads a property a parsed JSON object may lack, as null.
        Strict mode throws on a missing property, and mod authors omit fields freely.
    #>
    param(
        [object] $Object,
        [string] $Name
    )

    if ($null -eq $Object) {
        return $null
    }

    $property = $Object.PSObject.Properties[$Name]

    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}

function Get-VersionObjectParts {
    # The major, minor and patch of the { major, minor, patch } version form, a missing one as null.
    param([object] $Version)

    return ,@(
        (Get-JsonPropertyValue -Object $Version -Name 'major'),
        (Get-JsonPropertyValue -Object $Version -Name 'minor'),
        (Get-JsonPropertyValue -Object $Version -Name 'patch')
    )
}

function Remove-JsonComment {
    <#
        Strips # and // line comments that sit outside string literals.
        A regex cannot do this safely in either direction: matching anywhere
        destroys the "//" in a URL and the "#" in a hex colour, while matching
        only whole lines misses the comments that trail data on the same line
        (which is how most mod_info.json files annotate their fields).
    #>
    param([string] $Text)

    $builder = [System.Text.StringBuilder]::new($Text.Length)
    $isInString = $false
    $isEscaped = $false
    $index = 0

    while ($index -lt $Text.Length) {

        $char = $Text[$index]

        if ($isInString) {

            [void] $builder.Append($char)

            if ($isEscaped) {
                $isEscaped = $false
            } elseif ($char -eq '\') {
                $isEscaped = $true
            } elseif ($char -eq '"') {
                $isInString = $false
            }

            $index++
            continue
        }

        if ($char -eq '"') {

            $isInString = $true
            [void] $builder.Append($char)
            $index++
            continue
        }

        $isComment = ($char -eq '#') -or
            ($char -eq '/' -and ($index + 1) -lt $Text.Length -and $Text[$index + 1] -eq '/')

        if ($isComment) {

            while ($index -lt $Text.Length -and $Text[$index] -ne "`n") {
                $index++
            }

            continue
        }

        [void] $builder.Append($char)
        $index++
    }

    return $builder.ToString()
}

# Single-quoted scalar in value position: MagicLib and apex_design both write
# "major": '1' inside their version object.
$SingleQuotedValuePattern = "(?<=[:\[,{]\s{0,8})'([^']*)'(?=\s{0,8}[,\]}])"

# Unquoted bare word in value position, as in Thunder's Flag Pack's
# "patch":a. true/false/null are excluded so real literals survive.
$BareWordValuePattern = ':(\s*)(?!true\b|false\b|null\b)([A-Za-z_][\w.\-]*)(\s*[,}\]])'

function ConvertFrom-StarsectorJson {
    <#
        Starsector's JSON reader accepts input that ConvertFrom-Json rejects:
        BOMs, line comments, trailing commas, single-quoted scalars and bare
        unquoted words.

        Repairs are tried least-invasive first and never applied
        speculatively, because a rewrite can corrupt a file that did not need
        it. The single-quote pass is the cautionary case: it reads a pair of
        apostrophes in prose as a quoted scalar, which breaks Magellan
        Protectorate - a file that parses cleanly without it.
    #>
    param([string] $Path)

    $raw = [System.IO.File]::ReadAllText($Path)
    $raw = Remove-JsonComment -Text $raw
    $raw = [regex]::Replace($raw, ',(\s*[}\]])', '$1')

    $singleQuoted = [regex]::Replace($raw, $SingleQuotedValuePattern, '"$1"')
    $bareWord = [regex]::Replace($raw, $BareWordValuePattern, ':$1"$2"$3')
    $bothRepairs = [regex]::Replace($singleQuoted, $BareWordValuePattern, ':$1"$2"$3')

    $lastError = $null

    foreach ($candidate in @($raw, $singleQuoted, $bareWord, $bothRepairs)) {

        try {
            return $candidate | ConvertFrom-Json

        } catch {
            # The next, more invasive repair gets its turn; the throw below reports when none parses.
            $lastError = $_.Exception.Message
        }
    }

    throw "no repair produced valid JSON: $Path ($lastError)"
}

function Get-VersionSortKey {
    <#
        Builds an ordinally comparable key from either version shape: the
        "1.2.3" string form or the { major, minor, patch } object form.
        Components keep their leading digits only, so "91.DEV" ranks as 91
        and "1.3.1b" as 1 rather than collapsing to zero.
    #>
    param([object] $Version)

    if ($null -eq $Version) {
        $parts = @()

    } elseif ($Version -is [string] -or $Version -is [int] -or $Version -is [double]) {
        $parts = "$Version" -split '[.\-_ ]'

    } else {
        $parts = Get-VersionObjectParts -Version $Version
    }

    $key = foreach ($index in 0..($VersionPartCount - 1)) {

        $component = if ($index -lt $parts.Count) { "$($parts[$index])" } else { '0' }
        $leadingDigits = [regex]::Match($component.Trim(), '^\d+')
        $number = if ($leadingDigits.Success) { [int] $leadingDigits.Value } else { 0 }
        $number.ToString().PadLeft($VersionPadWidth, '0')
    }

    return ($key -join '.')
}

function Format-ModVersion {
    # Collapses both version shapes to one displayable string.
    param([object] $Version)

    if ($null -eq $Version) {
        return ''
    }

    if ($Version -is [string]) {
        return $Version
    }

    if ($null -ne (Get-JsonPropertyValue -Object $Version -Name 'major')) {

        $parts = Get-VersionObjectParts -Version $Version
        return (@($parts | Where-Object { $null -ne $_ }) -join '.')
    }

    return "$Version"
}

$modsRoot = Join-Path $StarsectorRoot 'mods'

if (-not (Test-Path $modsRoot)) {
    throw "mods directory not found: $modsRoot"
}

# An unreadable enabled_mods.json must not empty the roster, so fall back to
# treating every installed mod as live.
$enabledIds = @()
$enabledFile = Join-Path $modsRoot 'enabled_mods.json'

if (Test-Path $enabledFile) {

    try {

        # Read through the property, not Get-JsonPropertyValue: returning an empty list from a function unrolls it
        # to null, and an install with no mod enabled holds exactly that.
        $enabledProperty = (ConvertFrom-StarsectorJson -Path $enabledFile).PSObject.Properties['enabledMods']

        if ($null -eq $enabledProperty) {
            throw 'it holds no enabledMods list'
        }

        $enabledIds = @($enabledProperty.Value | Where-Object { $_ })

    } catch {

        Write-Warning "enabled_mods.json unparseable, treating all mods as enabled: $($_.Exception.Message)"
        $IncludeDisabled = $true
    }

} else {

    Write-Warning "enabled_mods.json not found, treating all mods as enabled."
    $IncludeDisabled = $true
}

$enabledSet = [System.Collections.Generic.HashSet[string]]::new(
    [string[]] $enabledIds,
    [System.StringComparer]::OrdinalIgnoreCase)

$candidates = [System.Collections.Generic.List[object]]::new()

foreach ($dir in Get-ChildItem -Path $modsRoot -Directory) {

    $infoPath = Join-Path $dir.FullName 'mod_info.json'

    if (-not (Test-Path $infoPath)) {
        continue
    }

    $info = $null
    $parseError = $null

    try {
        $info = ConvertFrom-StarsectorJson -Path $infoPath

    } catch {
        $parseError = $_.Exception.Message
    }

    $modId = Get-JsonPropertyValue -Object $info -Name 'id'

    if (-not $modId) {

        $reason = if ($parseError) { $parseError } else { 'no "id" field' }

        Write-Warning "Unparseable mod_info.json, keeping in roster: $($dir.Name) ($reason)"

        $candidates.Add([pscustomobject]@{
            Id      = $dir.Name
            Version = $null
            SortKey = ''
            Folder  = $dir
            Status  = $StatusUnparsed
        })

        continue
    }

    $version = Get-JsonPropertyValue -Object $info -Name 'version'

    $candidates.Add([pscustomobject]@{
        Id      = [string] $modId
        Version = $version
        SortKey = Get-VersionSortKey -Version $version
        Folder  = $dir
        Status  = $StatusParsed
    })
}

$roster = [System.Collections.Generic.List[object]]::new()
$supersededCount = 0
$disabledCount = 0

foreach ($group in ($candidates | Group-Object -Property Id)) {

    $ordered = $group.Group | Sort-Object -Property SortKey -Descending
    $winner = $ordered | Select-Object -First 1
    $supersededCount += $group.Count - 1

    # Unparsed entries bypass the enabled filter: their id is unknown, so the
    # only safe assumption is that they might matter.
    if ($winner.Status -eq $StatusParsed -and -not $IncludeDisabled -and -not $enabledSet.Contains($winner.Id)) {

        $disabledCount++
        continue
    }

    $roster.Add([pscustomobject]@{
        Id              = $winner.Id
        Version         = Format-ModVersion -Version $winner.Version
        Folder          = $winner.Folder.FullName
        RelativePath    = $winner.Folder.FullName.Substring($StarsectorRoot.Length).TrimStart('\')
        Status          = $winner.Status
        SupersededCount = $group.Count - 1
    })
}

$unparsedCount = @($roster | Where-Object { $_.Status -eq $StatusUnparsed }).Count

Write-DiagnosticLine ("read $($candidates.Count) mod folders under $modsRoot and $($enabledIds.Count) enabled IDs; " +
    "listing $($roster.Count) mods ($unparsedCount unparsed), " +
    "skipping $disabledCount disabled and $supersededCount superseded copies")

$roster
