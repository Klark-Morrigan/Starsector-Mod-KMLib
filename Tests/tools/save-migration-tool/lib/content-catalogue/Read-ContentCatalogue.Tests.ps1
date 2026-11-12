BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\catalogue-cache\Get-InstallCacheFolder.ps1"
    . "$libRoot\catalogue-cache\Read-CatalogueCacheOwners.ps1"
    . "$libRoot\catalogue-cache\Read-CatalogueCacheStatus.ps1"
    . "$libRoot\catalogue-cache\Write-CatalogueCacheEntry.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\content-catalogue\definitions\Get-ContentDefinitions.ps1"
    . "$libRoot\content-catalogue\readers\Get-ContentSources.ps1"
    . "$libRoot\content-catalogue\Format-SkippedFileLines.ps1"
    . "$libRoot\content-catalogue\New-ContentCatalogue.ps1"
    . "$libRoot\content-catalogue\Read-ContentCatalogue.ps1"

    function script:New-KindRead {
        # One kind's read, as New-ContentCatalogue returns it.
        param(
            [string] $Kind,
            [long] $Milliseconds,
            [int] $IdCount
        )

        return [pscustomobject]@{
            Kind = $Kind;
            Milliseconds = $Milliseconds;
            IdCount = $IdCount }
    }

    function script:New-KindStatus {
        # One kind's cache status, as Read-CatalogueCacheStatus returns it: read for a reason, or cached.
        param(
            [string] $Kind,
            [string] $RebuildReason,
            [int] $CachedGeneratedCount
        )

        $cachedEntry = if ($RebuildReason) {
            $null
        } else {
            [pscustomobject]@{
                Key = "cached $Kind key";
                GeneratedCount = $CachedGeneratedCount }
        }

        return [pscustomobject]@{
            Kind = $Kind
            Key = "$Kind key"
            RebuildReason = $RebuildReason
            CachedEntry = $cachedEntry }
    }
}

Describe 'Read-ContentCatalogue' {

    BeforeEach {

        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()
        $script:Written = [System.Collections.Generic.List[string]]::new()

        $script:HullOwners = New-OwnerIndex @{
            'a' = @('alpha');
            'b' = @('core') }

        $script:HullmodOwners = New-OwnerIndex @{ 'c' = @('core') }

        $script:CachedHullmodOwners = New-OwnerIndex @{
            'c' = @('core');
            'd' = @('alpha') }

        $script:KindReads = @(
            (New-KindRead 'Hull' 30 2),
            (New-KindRead 'Hullmod' 50 1))

        # Every kind needs reading, until a case says otherwise.
        $script:Statuses = @(
            (New-KindStatus 'Hull' 'not cached yet')
            (New-KindStatus 'Hullmod' 'not cached yet')
        )

        Mock Write-DiagnosticLine { $script:Diagnostics.Add("${Source}: $Message") }
        Mock Format-ConsoleText { $Text }
        Mock Format-ConsoleNumber { "$Number" }
        Mock Get-ContentSources { 'alpha source'; 'core source' }
        Mock Get-InstallCacheFolder { 'C:\cache' }
        Mock Read-CatalogueCacheOwners { $script:CachedHullmodOwners }
        Mock Write-CatalogueCacheEntry { $script:Written.Add("$Kind $Key $($OwnersById.Count) $GeneratedCount") }

        Mock Get-ContentDefinitions {
            foreach ($kind in
                    'Hull',
                    'Hullmod',
                    'Faction') {

                [pscustomobject]@{ Kind = $kind }
            }
        }

        Mock Read-CatalogueCacheStatus {

            [pscustomobject]@{
                Statuses = $script:Statuses;
                DataFileCount = 7 }
        }

        # One line per skipped path, so a case reads back what it was handed.
        Mock Format-SkippedFileLines {

            foreach ($skippedFile in $SkippedFiles) {
                "skipped line for $($skippedFile.Path)" }
        }

        Mock New-ContentCatalogue {

            $ownersByKind = [ordered]@{}
            $generatedCountByKind = @{}

            foreach ($kind in $Kinds) {

                $ownersByKind[$kind] = if ($kind -eq 'Hull') {
                    $script:HullOwners
                } else {
                    $script:HullmodOwners
                }

                $generatedCountByKind[$kind] = 1
            }

            [pscustomobject]@{

                OwnersByKind = $ownersByKind
                GeneratedCountByKind = $generatedCountByKind
                ContentFileCount = 120
                SpreadsheetCount = 4

                SkippedFiles = @([pscustomobject]@{
                    Path = 'C:\mods\alpha\data\hulls\bad.ship';
                    Reason = 'broken' })

                Replacements = @([pscustomobject]@{
                    Path = 'data/hulls/ship_data.csv';
                    Owner = 'alpha' })

                KindReads = $script:KindReads
            }
        }
    }

    It 'reads the kinds the cache cannot serve, and returns each kind''s owners and the sources' {

        # ARRANGE
        $expectedKinds = @(
            'Hull',
            'Hullmod')
        $expectedSources = @(
            'alpha source',
            'core source')

        # ACT
        $actualCatalogue = Read-ContentCatalogue `
            -StarsectorRoot 'C:\Games' `
            -Roster @('one mod') `
            -Kinds @(
                'Hull',
                'Hullmod') `
            -DiagnosticSource 'Test'

        # ASSERT
        @($actualCatalogue.OwnersByKind.Keys) |
            Should -Be $expectedKinds

        [object]::ReferenceEquals(
                $actualCatalogue.OwnersByKind['Hull'],
                $script:HullOwners) |
            Should -BeTrue

        $actualCatalogue.Sources |
            Should -Be $expectedSources

        Should -Invoke Get-ContentSources `
            -ParameterFilter {
                $StarsectorRoot -eq 'C:\Games' `
                    -and "$Roster" -eq 'one mod'
            }

        Should -Invoke New-ContentCatalogue `
            -ParameterFilter {
                "$Sources" -eq 'alpha source core source' `
                    -and "$Kinds" -eq 'Hull Hullmod'
            } `
            -Times 1 `
            -Exactly

        Should -Invoke Read-CatalogueCacheOwners `
            -Times 0 `
            -Exactly
    }

    It 'asks the install''s cache about the kinds in the definitions'' order, however they were asked for' {

        # ARRANGE
        $expectedKinds = 'Hull Faction'

        # ACT
        Read-ContentCatalogue `
            -StarsectorRoot 'C:\Games' `
            -Roster @() `
            -Kinds @(
                'Faction',
                'Hull') `
            -IsRebuildForced `
            -DiagnosticSource 'Test' |
            Out-Null

        # ASSERT
        Should -Invoke Get-InstallCacheFolder `
            -ParameterFilter { $StarsectorRoot -eq 'C:\Games' } `
            -Times 1 `
            -Exactly

        Should -Invoke Read-CatalogueCacheStatus `
            -ParameterFilter {
                $CacheFolder -eq 'C:\cache' `
                    -and "$Kinds" -eq $expectedKinds `
                    -and $IsRebuildForced `
                    -and "$Sources" -eq 'alpha source core source'
            } `
            -Times 1 `
            -Exactly
    }

    It 'caches each kind it read, under that kind''s key, with how many of its IDs the game builds' {

        # ARRANGE
        $expectedWritten = @(
            'Hull Hull key 2 1',
            'Hullmod Hullmod key 1 1')

        # ACT
        Read-ContentCatalogue `
            -StarsectorRoot 'C:\Games' `
            -Roster @() `
            -Kinds @(
                'Hull',
                'Hullmod') `
            -DiagnosticSource 'Test' |
            Out-Null

        # ASSERT
        $script:Written |
            Should -Be $expectedWritten

        Should -Invoke Write-CatalogueCacheEntry `
            -ParameterFilter { $CacheFolder -eq 'C:\cache' } `
            -Times 2 `
            -Exactly
    }

    It 'takes a current kind from the cache, reading only the others, and keeps the kinds'' order' {

        # ARRANGE
        $script:Statuses = @(
            (New-KindStatus 'Hull' 'the data files changed')
            (New-KindStatus 'Hullmod' '' 3)
        )

        $expectedKinds = @(
            'Hull',
            'Hullmod')

        $expectedWritten = @('Hull Hull key 2 1')

        # ACT
        $actualCatalogue = Read-ContentCatalogue `
            -StarsectorRoot 'C:\Games' `
            -Roster @() `
            -Kinds @(
                'Hull',
                'Hullmod') `
            -DiagnosticSource 'Test'

        # ASSERT
        @($actualCatalogue.OwnersByKind.Keys) |
            Should -Be $expectedKinds

        [object]::ReferenceEquals(
                $actualCatalogue.OwnersByKind['Hullmod'],
                $script:CachedHullmodOwners) |
            Should -BeTrue

        $script:Written |
            Should -Be $expectedWritten

        Should -Invoke New-ContentCatalogue `
            -ParameterFilter { "$Kinds" -eq 'Hull' } `
            -Times 1 `
            -Exactly

        Should -Invoke Read-CatalogueCacheOwners `
            -ParameterFilter {
                $CacheFolder -eq 'C:\cache' `
                    -and $Kind -eq 'Hullmod'
            } `
            -Times 1 `
            -Exactly
    }

    It 'reads nothing when the cache serves every kind' {

        # ARRANGE
        $script:Statuses = @((New-KindStatus 'Hullmod' '' 3))

        # ACT
        Read-ContentCatalogue `
            -StarsectorRoot 'C:\Games' `
            -Roster @() `
            -Kinds @('Hullmod') `
            -DiagnosticSource 'Test' |
            Out-Null

        # ASSERT
        Should -Invoke New-ContentCatalogue `
            -Times 0 `
            -Exactly

        Should -Invoke Write-CatalogueCacheEntry `
            -Times 0 `
            -Exactly
    }

    It 'says what it catalogues, what the cache held, why each other kind was read, and what the read found' {

        # ARRANGE
        $script:Statuses = @(
            (New-KindStatus 'Hull' 'the data files changed')
            (New-KindStatus 'Hullmod' '' 3)
            (New-KindStatus 'Faction' 'the data files changed')
        )

        $expectedDiagnostics = @(
            "Test: cataloguing Hull, Hullmod, Faction from 1 mods and the game's own data, in the game's search order"
            'Test: checked 7 data files against the cache in C:\cache, in * ms'
            'Test: loaded from the cache: Hullmod'
            'Test: reading Hull, Faction: the data files changed'
            'Test: skipped line for C:\mods\alpha\data\hulls\bad.ship'
            'Test: alpha replaces data/hulls/ship_data.csv, so no other copy of it is read'
            'Test: read 120 content files and 4 spreadsheets in * ms, skipping 1'
            'Test: slowest to read: Hullmod in 50 ms (1 IDs), Hull in 30 ms (2 IDs)'
            'Test: cached Hull, Faction'
            'Test: defined: 2 Hull, 2 Hullmod, 1 Faction, 5 of them built by the game at startup'
        )

        # ACT
        Read-ContentCatalogue `
            -StarsectorRoot 'C:\Games' `
            -Roster @('one mod') `
            -Kinds @(
                'Hull',
                'Hullmod',
                'Faction') `
            -DiagnosticSource 'Test' |
            Out-Null

        # ASSERT
        $script:Diagnostics.
            Count |
            Should -Be $expectedDiagnostics.Count

        for ($index = 0; $index -lt $expectedDiagnostics.Count; $index++) {

            $script:Diagnostics[$index] |
                Should -BeLike $expectedDiagnostics[$index]
        }
    }

    It 'gives one line per reason when kinds are read for different reasons' {

        # ARRANGE
        $script:Statuses = @(
            (New-KindStatus 'Hull' 'not cached yet')
            (New-KindStatus 'Hullmod' 'the mods changed')
        )

        $expectedReasonLines = @(
            'Test: reading Hull: not cached yet',
            'Test: reading Hullmod: the mods changed')

        # ACT
        Read-ContentCatalogue `
            -StarsectorRoot 'C:\Games' `
            -Roster @() `
            -Kinds @(
                'Hull',
                'Hullmod') `
            -DiagnosticSource 'Test' |
            Out-Null

        # ASSERT
        @($script:Diagnostics `
            -like 'Test: reading*') |
            Should -Be $expectedReasonLines
    }

    # 45 kinds would make a long line, and the few slowest are where a slow run spent its time.
    It 'names only the five slowest kinds, slowest first, a tie in the order read' {

        # ARRANGE
        $script:KindReads = @(
            (New-KindRead 'A' 10 1)
            (New-KindRead 'B' 60 1)
            (New-KindRead 'C' 30 1)
            (New-KindRead 'D' 40 1)
            (New-KindRead 'E' 60 1)
            (New-KindRead 'F' 20 1)
        )

        $expectedSlowest = (
            'Test: slowest to read: B in 60 ms (1 IDs), E in 60 ms (1 IDs), D in 40 ms (1 IDs), ' +
            'C in 30 ms (1 IDs), F in 20 ms (1 IDs)')

        # ACT
        Read-ContentCatalogue `
            -StarsectorRoot 'C:\Games' `
            -Roster @() `
            -Kinds @('Hull') `
            -DiagnosticSource 'Test' |
            Out-Null

        # ASSERT
        @($script:Diagnostics `
            -like 'Test: slowest*') |
            Should -Be @($expectedSlowest)
    }
}
