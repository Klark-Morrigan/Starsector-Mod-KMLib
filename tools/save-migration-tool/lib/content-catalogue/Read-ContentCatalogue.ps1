# Reads which content IDs the install defines, per kind, from the game's own data and every enabled mod's,
# and says on stderr what it read, what it skipped, what mods replace, how much each kind holds
# and which kinds took longest to read.
# Returns the owners of each ID per kind, as New-ContentCatalogue builds them,
# and the sources they were read from, in search order, for a caller that names or orders the owners.
function Read-ContentCatalogue {

    [CmdletBinding()]
    param(
        [string] $StarsectorRoot,

        # The install's enabled mods, as Read-ModRoster lists them.
        [object[]] $Roster,

        # The kinds to catalogue, as Get-ContentKindNames names them.
        [string[]] $Kinds,

        # The script name each diagnostic line opens with.
        [string] $DiagnosticSource
    )

    # Enough kinds to show where a slow run spent its time, few enough to keep the line short.
    $slowestKindCount = 5

    $sources = @(Get-ContentSources -StarsectorRoot $StarsectorRoot -Roster $Roster)

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "cataloguing $($Kinds -join ', ') from $(Format-ConsoleNumber $Roster.Count) mods and the game's own data, " +
        "in the game's search order")

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $catalogue = New-ContentCatalogue -Sources $sources -Kinds $Kinds

    foreach ($skippedLine in Format-SkippedFileLines -SkippedFiles $catalogue.SkippedFiles) {
        Write-DiagnosticLine -Source $DiagnosticSource -Message $skippedLine
    }

    foreach ($replacement in $catalogue.Replacements) {

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "$(Format-ConsoleText -Role 'Mod' -Text $replacement.Owner) replaces " +
            "$(Format-ConsoleText -Role 'Path' -Text $replacement.Path), so no other copy of it is read")
    }

    $kindCounts = foreach ($kind in $catalogue.OwnersByKind.Keys) {
        "$(Format-ConsoleNumber $catalogue.OwnersByKind[$kind].Count) $kind"
    }

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $(Format-ConsoleNumber $catalogue.ContentFileCount) content files " +
        "and $(Format-ConsoleNumber $catalogue.SpreadsheetCount) spreadsheets " +
        "in $(Format-ConsoleText -Role 'Time' -Text "$($stopwatch.ElapsedMilliseconds) ms"), " +
        "skipping $(Format-ConsoleNumber $catalogue.SkippedFiles.Count); " +
        "defined: $($kindCounts -join ', '), " +
        "$(Format-ConsoleNumber $catalogue.GeneratedCount) of them built by the game at startup")

    $slowestKindReads = $catalogue.KindReads |
        Sort-Object -Property 'Milliseconds' -Descending -Stable |
        Select-Object -First $slowestKindCount

    $slowestKinds = foreach ($kindRead in $slowestKindReads) {

        "$($kindRead.Kind) in $(Format-ConsoleText -Role 'Time' -Text "$($kindRead.Milliseconds) ms") " +
        "($(Format-ConsoleNumber $kindRead.IdCount) IDs)"
    }

    Write-DiagnosticLine -Source $DiagnosticSource -Message "slowest to read: $($slowestKinds -join ', ')"

    return [pscustomobject]@{
        OwnersByKind = $catalogue.OwnersByKind
        Sources      = $sources
    }
}
