# Decides, for each kind asked for, whether its cached catalogue is current or must be rebuilt, and why.
# A kind's key covers its own data files and those of the kinds the game builds it from,
# since a variant the game builds from a hull changes with the hull's files.
# Returns one status per kind, in the order asked: the key a rebuild writes under,
# and either the reason the kind must be read again or its current cache entry.
# Returns too how many data files the keys cover, each counted once.
function Read-CatalogueCacheStatus {

    param(
        # The install's cache folder, as Get-InstallCacheFolder names it.
        [string] $CacheFolder,

        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources,

        # The kinds asked for, as Get-ContentKindNames names them.
        [string[]] $Kinds,

        # Rebuild every kind, whatever the cache holds.
        [switch] $IsRebuildForced
    )

    $definitionsByKind = @{}

    foreach ($definition in Get-ContentDefinitions) {
        $definitionsByKind[$definition.Kind] = $definition
    }

    $codeHash = Get-CatalogueCodeHash
    $sourcesHash = Get-ContentSourcesHash -Sources $Sources
    $cache = New-ParsedFileCache
    $inputFilesByKind = @{}
    $dataFiles = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase)

    $statuses = foreach ($kind in $Kinds) {

        $definition = $definitionsByKind[$kind]
        $readKinds = @($kind) + @(foreach ($rule in $definition.Generated) { $rule.FromKind })

        $kindFiles = foreach ($readKind in $readKinds |
                Select-Object -Unique) {

            if (-not $inputFilesByKind.ContainsKey($readKind)) {

                $inputFilesByKind[$readKind] = @(Get-ContentKindInputFiles `
                    -Sources $Sources `
                    -Definition $definitionsByKind[$readKind] `
                    -Cache $cache)
            }

            $inputFilesByKind[$readKind]
        }

        foreach ($kindFile in $kindFiles) {
            $dataFiles.Add($kindFile) | Out-Null
        }

        $key = New-CatalogueCacheKey `
            -CodeHash $codeHash `
            -SourcesHash $sourcesHash `
            -DataFilePaths $kindFiles

        $entry = $null
        $reason = $null

        if ($IsRebuildForced) {
            $reason = 'asked to rebuild'

        } else {

            try {
                $entry = Read-CatalogueCacheEntry `
                    -CacheFolder $CacheFolder `
                    -Kind $kind

            } catch {
                $reason = "its cache entry does not parse: $($_.Exception.Message)"
            }
        }

        if (-not $reason `
                -and $null -eq $entry) {

            $reason = 'not cached yet'
        }

        if (-not $reason) {

            $stalePart = Find-StaleCacheKeyPart `
                -CachedKey $entry.Key `
                -CurrentKey $key

            if ($stalePart) {
                $reason = "$stalePart changed"
            }
        }

        # A stale entry is of no further use: the rebuild counts afresh.
        $cachedEntry = if ($reason) {
            $null
        } else {
            $entry
        }

        [pscustomobject]@{
            Kind = $kind
            Key = $key
            RebuildReason = $reason
            CachedEntry = $cachedEntry
        }
    }

    return [pscustomobject]@{
        Statuses = @($statuses)
        DataFileCount = $dataFiles.Count
    }
}
