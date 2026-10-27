# Reads which content IDs the install defines, per kind, from the game's own data and every enabled mod's,
# and says on stderr what it read, what it skipped and how much each kind holds.
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

    $sources = @(Get-ContentSources -StarsectorRoot $StarsectorRoot -Roster $Roster)

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "cataloguing $($Kinds -join ', ') from $(Format-ConsoleNumber $Roster.Count) mods and the game's own data, " +
        "in the game's search order")

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $catalogue = New-ContentCatalogue -Sources $sources -Kinds $Kinds

    foreach ($skippedFile in $catalogue.SkippedFiles) {

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "skipped $(Format-ConsoleText -Role 'Path' -Text $skippedFile.Path): $($skippedFile.Reason)")
    }

    foreach ($replacement in $catalogue.Replacements) {

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "$(Format-ConsoleText -Role 'Mod' -Text $replacement.Owner) replaces " +
            "$(Format-ConsoleText -Role 'Path' -Text $replacement.Spreadsheet), so no other copy of it is read")
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

    return [pscustomobject]@{
        OwnersByKind = $catalogue.OwnersByKind
        Sources      = $sources
    }
}
