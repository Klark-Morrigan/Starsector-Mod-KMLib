# Reads which content IDs the install defines, per kind, from the game's own data and every enabled mod's,
# and says on stderr what it read, what it skipped, what mods replace, how much each kind holds
# and which kinds took longest to read.
# Returns the owners of each ID per kind, as New-ContentCatalogue builds them,
# and the sources they were read from, in search order, for a caller that names or orders the owners.
#
# Reading every kind takes a minute or more on a large install, so each kind is kept in the install's cache.
# A kind is read again only when its cached key differs: the catalogue's code, the mods, or its data files.
# Stderr says which kinds came from the cache, and why each other one was read.
# What a read skipped or found replaced is said only for the kinds read in this run.
function Read-ContentCatalogue {

    [CmdletBinding()]
    param(
        [string] $StarsectorRoot,

        # The install's enabled mods, as Read-ModRoster lists them.
        [object[]] $Roster,

        # The kinds to catalogue, as Get-ContentKindNames names them.
        [string[]] $Kinds,

        # Read every kind asked for, whatever the cache holds.
        [switch] $IsRebuildForced,

        # The script name each diagnostic line opens with.
        [string] $DiagnosticSource
    )

    # Enough kinds to show where a slow run spent its time, few enough to keep the line short.
    $slowestKindCount = 5

    $sources = @(Get-ContentSources `
        -StarsectorRoot $StarsectorRoot `
        -Roster $Roster)

    # The kinds keep the definitions' order, however they were asked for.
    $orderedKinds = @((Get-ContentDefinitions).
        Kind |
        Where-Object { $_ -cin $Kinds })

    Write-DiagnosticLine `
        -Source $DiagnosticSource `
        -Message (
            "cataloguing $($orderedKinds -join ', ') from $(Format-ConsoleNumber $Roster.Count) mods " +
            "and the game's own data, in the game's search order")

    $cacheFolder = Get-InstallCacheFolder -StarsectorRoot $StarsectorRoot
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

    $cacheStatus = Read-CatalogueCacheStatus `
        -CacheFolder $cacheFolder `
        -Sources $sources `
        -Kinds $orderedKinds `
        -IsRebuildForced:$IsRebuildForced

    Write-DiagnosticLine `
        -Source $DiagnosticSource `
        -Message (
            "checked $(Format-ConsoleNumber $cacheStatus.DataFileCount) data files against the cache in " +
            "$(Format-ConsoleText -Role 'Path' -Text $cacheFolder), " +
            "in $(Format-ConsoleText -Role 'Time' -Text "$($stopwatch.ElapsedMilliseconds) ms")")

    $cachedStatuses = @($cacheStatus.
        Statuses |
        Where-Object { -not $_.RebuildReason })

    $rebuiltStatuses = @($cacheStatus.
        Statuses |
        Where-Object RebuildReason)

    if ($cachedStatuses.Count -gt 0) {

        Write-DiagnosticLine `
            -Source $DiagnosticSource `
            -Message ("loaded from the cache: $($cachedStatuses.Kind -join ', ')")
    }

    foreach ($reasonGroup in $rebuiltStatuses |
            Group-Object -Property 'RebuildReason') {

        Write-DiagnosticLine `
            -Source $DiagnosticSource `
            -Message ("reading $($reasonGroup.Group.Kind -join ', '): $($reasonGroup.Name)")
    }

    $ownersByRebuiltKind = @{}
    $generatedCountByKind = @{}

    foreach ($cachedStatus in $cachedStatuses) {

        $generatedCountByKind[$cachedStatus.Kind] = $cachedStatus.
            CachedEntry.
            GeneratedCount
    }

    if ($rebuiltStatuses.Count -gt 0) {

        $stopwatch.Restart()

        $catalogue = New-ContentCatalogue `
            -Sources $sources `
            -Kinds $rebuiltStatuses.Kind

        foreach ($skippedLine in Format-SkippedFileLines `
                -SkippedFiles $catalogue.SkippedFiles) {

            Write-DiagnosticLine `
                -Source $DiagnosticSource `
                -Message $skippedLine
        }

        foreach ($replacement in $catalogue.Replacements) {

            Write-DiagnosticLine `
                -Source $DiagnosticSource `
                -Message (
                    "$(Format-ConsoleText -Role 'Mod' -Text $replacement.Owner) replaces " +
                    "$(Format-ConsoleText -Role 'Path' -Text $replacement.Path), so no other copy of it is read")
        }

        Write-DiagnosticLine `
            -Source $DiagnosticSource `
            -Message (
                "read $(Format-ConsoleNumber $catalogue.ContentFileCount) content files " +
                "and $(Format-ConsoleNumber $catalogue.SpreadsheetCount) spreadsheets " +
                "in $(Format-ConsoleText -Role 'Time' -Text "$($stopwatch.ElapsedMilliseconds) ms"), " +
                "skipping $(Format-ConsoleNumber $catalogue.SkippedFiles.Count)")

        $slowestKindReads = $catalogue.KindReads |
            Sort-Object `
                -Property 'Milliseconds' `
                -Descending `
                -Stable |
            Select-Object `
                -First $slowestKindCount

        $slowestKinds = foreach ($kindRead in $slowestKindReads) {

            "$($kindRead.Kind) in $(Format-ConsoleText -Role 'Time' -Text "$($kindRead.Milliseconds) ms") " +
            "($(Format-ConsoleNumber $kindRead.IdCount) IDs)"
        }

        Write-DiagnosticLine `
            -Source $DiagnosticSource `
            -Message "slowest to read: $($slowestKinds -join ', ')"

        foreach ($rebuiltStatus in $rebuiltStatuses) {

            $kind = $rebuiltStatus.Kind
            $ownersByRebuiltKind[$kind] = $catalogue.OwnersByKind[$kind]
            $generatedCountByKind[$kind] = $catalogue.GeneratedCountByKind[$kind]

            Write-CatalogueCacheEntry `
                -CacheFolder $cacheFolder `
                -Kind $kind `
                -Key $rebuiltStatus.Key `
                -OwnersById $ownersByRebuiltKind[$kind] `
                -GeneratedCount $generatedCountByKind[$kind]
        }

        Write-DiagnosticLine `
            -Source $DiagnosticSource `
            -Message "cached $($rebuiltStatuses.Kind -join ', ')"
    }

    $ownersByKind = [ordered]@{}

    foreach ($status in $cacheStatus.Statuses) {

        $ownersByKind[$status.Kind] = if ($status.RebuildReason) {

            $ownersByRebuiltKind[$status.Kind]

        } else {
            Read-CatalogueCacheOwners `
                -CacheFolder $cacheFolder `
                -Kind $status.Kind
        }
    }

    $kindCounts = foreach ($kind in $ownersByKind.Keys) {
        "$(Format-ConsoleNumber $ownersByKind[$kind].Count) $kind"
    }

    $generatedCount = ($generatedCountByKind.Values |
        Measure-Object -Sum).
        Sum

    Write-DiagnosticLine `
        -Source $DiagnosticSource `
        -Message (
            "defined: $($kindCounts -join ', '), " +
            "$(Format-ConsoleNumber $generatedCount) of them built by the game at startup")

    return [pscustomobject]@{
        OwnersByKind = $ownersByKind
        Sources = $sources
    }
}
