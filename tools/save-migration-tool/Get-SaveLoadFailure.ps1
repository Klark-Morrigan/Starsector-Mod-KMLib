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

# The game's log location relative to the install root.
$DefaultLogRelativePath = 'starsector-core\starsector.log'

# Every load line comes from this logger; its name gates the cheap pre-check on each log line.
$LoaderLoggerName = 'com.fs.starfarer.campaign.save.CampaignGameManager'

# A log line from the loader: "<ms> [<thread>] <LEVEL> <logger>  - <message>".
# Anchoring on the timestamp matters: the loader's own stack frames name the same class.
$LoaderLogLinePattern = "^\d+ \[[^\]]*\] (?<level>\w+)\s+$([regex]::Escape($LoaderLoggerName))\s+- (?<message>.*)$"

$AttemptStartPattern = '^Loading (?<path>.+)\.\.\.$'
$AttemptSuccessMessage = 'Finished loading'
$AttemptFailureMessage = 'Error loading'

# The loader logs its exception at this level, then prints the trace beneath it.
$ExceptionLogLevel = 'ERROR'

# A trace's own header and each "Caused by:" line name a throwable at column 0.
# Frames and "Suppressed:" blocks are indented, and log lines open with a timestamp, so neither matches.
$ThrowableHeaderPattern =
    '^(?:Caused by: )?(?<type>(?:[A-Za-z_$][\w$]*\.)+[\w$]*(?:Exception|Error|Throwable))(?:: (?<message>.*))?$'

# The files a load reads, in the order vanilla tries them.
# The load method calls itself again on failure, first with its bak flag, then with its inprogress flag.
# campaign stands for campaign.xml, or campaign.zip for a compressed save.
$AttemptSources = @('campaign', 'campaign.bak', 'campaign.inprogress')

$OutcomeLoaded = 'Loaded'
$OutcomeFailed = 'Failed'

# The log ends before the attempt does: the game crashed or is still loading.
$OutcomeUnfinished = 'Unfinished'

# The message each spec store throws when a save names an id it lacks, one wording per store.
# No two patterns match one message, so their order does not matter.
$MissingSpecPatterns = @(
    @{ Kind = 'Weapon'; Pattern = '^Weapon spec \[(?<id>[^\]]+)\] not found!$' }
    @{ Kind = 'Hull'; Pattern = '^Ship hull spec \[(?<id>[^\]]+)\] not found!$' }
    @{ Kind = 'Variant'; Pattern = '^Ship hull variant \[(?<id>[^\]]+)\] not found!$' }
    @{ Kind = 'FighterWing'; Pattern = '^Fighter wing spec \[(?<id>[^\]]+)\] not found$' }
    @{ Kind = 'Hullmod'; Pattern = '^Hullmod with id \[(?<id>[^\]]+)\] not found$' }

    # Every other spec type shares one generic store, whose message names the spec's class instead of a kind.
    @{ Kind = 'Spec'; Pattern = '^Spec of class \[(?<class>[^\]]+)\] with id \[(?<id>[^\]]+)\] not found$' }
)

# XStream throws this when a save names a class no loaded jar holds; its message is the class name alone.
$UnresolvedClassExceptionType = 'com.thoughtworks.xstream.mapper.CannotResolveClassException'
$MissingClassKind = 'Class'

$HullKind = 'Hull'

# Vanilla builds every hull's d-hull at startup, under the hull id plus this suffix (Misc.D_HULL_SUFFIX).
# A missing d-hull so means its base hull is missing, and the base hull is what a mod defines.
$DHullSuffix = '_default_D'

function Write-DiagnosticLine {
    # Stderr keeps stdout parseable when -AsJson is set.
    param([string] $Message)

    [Console]::Error.WriteLine("Get-SaveLoadFailure: $Message")
}

function Get-LogFileChain {
    <#
        Returns the log and its rotated siblings, oldest first.
        log4j rotation shifts starsector.log to .1 and each .N to .N+1, so the highest number is the oldest.
    #>
    param([string] $Path)

    $directory = Split-Path -Parent $Path
    $leafName = Split-Path -Leaf $Path
    $rotatedPattern = "^$([regex]::Escape($leafName))\.(?<index>\d+)$"

    $rotated = Get-ChildItem -LiteralPath $directory -File |
        Where-Object { $_.Name -match $rotatedPattern } |
        Sort-Object -Property { [int] ([regex]::Match($_.Name, $rotatedPattern).Groups['index'].Value) } -Descending

    # Comma-operator wrap so a log with no rotated siblings still returns an array, not a lone string.
    return ,@(@($rotated | ForEach-Object { $_.FullName }) + $Path)
}

function Read-LoadAttempt {
    <#
        Streams the log files as one log and returns every load attempt in order.
        An attempt's lines may straddle a rotation, so the parse state carries across files.
    #>
    param(
        [string[]] $LogFiles,
        [string] $SaveBaseDirectory
    )

    $attempts = [System.Collections.Generic.List[object]]::new()
    $currentAttempt = $null
    $isReadingCause = $false

    foreach ($logFile in $LogFiles) {

        $lineNumber = 0
        $attemptCountBefore = $attempts.Count

        foreach ($line in [System.IO.File]::ReadLines($logFile)) {

            $lineNumber++
            $loaderLine = if ($line.Contains($LoaderLoggerName)) { [regex]::Match($line, $LoaderLogLinePattern) }
            $isLoaderLine = $null -ne $loaderLine -and $loaderLine.Success

            if ($isReadingCause) {
                if (-not $isLoaderLine) {

                    $header = [regex]::Match($line, $ThrowableHeaderPattern)

                    # Java prints the chain outermost first, so the last header is the root cause.
                    if ($header.Success) {
                        $currentAttempt.CauseType = $header.Groups['type'].Value
                        $currentAttempt.Cause = $header.Groups['message'].Value
                    }
                    continue
                }

                if ($loaderLine.Groups['level'].Value -eq $ExceptionLogLevel) { continue }

                $isReadingCause = $false
                $currentAttempt = $null
            }

            if (-not $isLoaderLine) {
                continue
            }

            $message = $loaderLine.Groups['message'].Value
            $start = [regex]::Match($message, $AttemptStartPattern)

            if ($start.Success) {

                $currentAttempt = [pscustomobject]@{
                    SavePath  = [System.IO.Path]::GetFullPath($start.Groups['path'].Value, $SaveBaseDirectory)
                    Outcome   = $OutcomeUnfinished
                    CauseType = $null
                    Cause     = $null
                    LogFile   = $logFile
                    LogLine   = $lineNumber
                }

                $attempts.Add($currentAttempt)
                continue
            }

            if ($null -eq $currentAttempt) {
                continue
            }

            if ($message -eq $AttemptSuccessMessage) {

                $currentAttempt.Outcome = $OutcomeLoaded
                $currentAttempt = $null

            } elseif ($message -eq $AttemptFailureMessage) {

                $currentAttempt.Outcome = $OutcomeFailed
                $isReadingCause = $true
            }
        }

        $foundCount = $attempts.Count - $attemptCountBefore

        Write-DiagnosticLine "read $logFile`: $lineNumber lines, $foundCount load attempts start here"
    }

    return $attempts
}

function Get-MissingContent {
    <#
        Reads which content a failure's root cause names: its kind and id.
        Id is what to look for in a mod; ReportedId is the id as the message names it.
        The two differ for a d-hull, which names its base hull.
        Returns null when the cause names no missing content.
    #>
    param(
        [string] $CauseType,
        [string] $Cause
    )

    if ($CauseType -eq $UnresolvedClassExceptionType) {
        return [pscustomobject]@{ Kind = $MissingClassKind; Id = $Cause; ReportedId = $Cause; SpecClass = $null }
    }

    foreach ($missingSpec in $MissingSpecPatterns) {

        $match = [regex]::Match($Cause, $missingSpec.Pattern)

        if (-not $match.Success) {
            continue
        }

        $reportedId = $match.Groups['id'].Value
        $isDHull = $missingSpec.Kind -eq $HullKind -and $reportedId.EndsWith($DHullSuffix)
        $id = if ($isDHull) { $reportedId.Substring(0, $reportedId.Length - $DHullSuffix.Length) } else { $reportedId }
        $specClass = $match.Groups['class']

        return [pscustomobject]@{
            Kind       = $missingSpec.Kind
            Id         = $id
            ReportedId = $reportedId
            SpecClass  = if ($specClass.Success) { $specClass.Value } else { $null }
        }
    }

    return $null
}

function Format-MissingContentLine {
    # One diagnostic line per load with a cause, so the log says what each cause was read as.
    param([object] $Load)

    $missing = $Load.Missing

    if ($null -eq $missing) {
        return "$($Load.SaveFolder): the cause names no missing content"
    }

    $line = "$($Load.SaveFolder): missing $($missing.Kind) $($missing.Id)"

    if ($missing.SpecClass) {
        $line += " of spec class $($missing.SpecClass)"
    }

    if ($missing.ReportedId -ne $missing.Id) {
        $line += ", named as $($missing.ReportedId)"
    }

    return $line
}

function Group-LoadAttempt {
    <#
        Folds vanilla's retries into the load that caused them.
        An attempt is a retry when the one before it failed on the same save
        and the load has not yet used every source.
        A player loading the same broken save twice so still reads as two loads.
    #>
    param([object[]] $Attempts)

    $loads = [System.Collections.Generic.List[object]]::new()
    $currentAttempts = $null

    foreach ($attempt in $Attempts) {

        $previous = if ($null -ne $currentAttempts) { $currentAttempts[$currentAttempts.Count - 1] } else { $null }
        $isRetry = $null -ne $previous -and
            $previous.Outcome -eq $OutcomeFailed -and
            $previous.SavePath -eq $attempt.SavePath -and
            $currentAttempts.Count -lt $AttemptSources.Count

        if (-not $isRetry) {

            $currentAttempts = [System.Collections.Generic.List[object]]::new()
            $loads.Add($currentAttempts)
        }

        $currentAttempts.Add($attempt)
    }

    foreach ($loadAttempts in $loads) {

        $firstAttempt = $loadAttempts[0]
        $sourcedAttempts = for ($index = 0; $index -lt $loadAttempts.Count; $index++) {

            $attempt = $loadAttempts[$index]

            [pscustomobject]@{
                Source    = $AttemptSources[$index]
                Outcome   = $attempt.Outcome
                CauseType = $attempt.CauseType
                Cause     = $attempt.Cause
                Missing   = Get-MissingContent -CauseType $attempt.CauseType -Cause $attempt.Cause
                LogFile   = $attempt.LogFile
                LogLine   = $attempt.LogLine
            }
        }

        $sourcedAttempts = @($sourcedAttempts)

        [pscustomobject]@{
            SaveFolder = Split-Path -Leaf $firstAttempt.SavePath
            SavePath   = $firstAttempt.SavePath
            Outcome    = $loadAttempts[$loadAttempts.Count - 1].Outcome
            CauseType  = $firstAttempt.CauseType
            Cause      = $firstAttempt.Cause
            Missing    = $sourcedAttempts[0].Missing
            Attempts   = $sourcedAttempts
        }
    }
}

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
Write-DiagnosticLine "reading $($logFiles.Count) log files, oldest first"

$attempts = @(Read-LoadAttempt -LogFiles $logFiles -SaveBaseDirectory $saveBaseDirectory)
$loads = @(Group-LoadAttempt -Attempts $attempts)
[array]::Reverse($loads)

$failedCount = @($loads | Where-Object { $_.Outcome -eq $OutcomeFailed }).Count

Write-DiagnosticLine ("grouped $($attempts.Count) attempts into $($loads.Count) loads, $failedCount failed; " +
    'listing them newest first')

foreach ($load in $loads | Where-Object { $_.CauseType }) {
    Write-DiagnosticLine (Format-MissingContentLine -Load $load)
}

if ($AsJson) {
    ConvertTo-Json -InputObject $loads -Depth 4

} else {
    $loads
}
