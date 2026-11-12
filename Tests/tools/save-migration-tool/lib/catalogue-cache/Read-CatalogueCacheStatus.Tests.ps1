BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    foreach ($name in @(
            'Find-StaleCacheKeyPart'
            'Get-CatalogueCodeHash'
            'Get-ContentKindInputFiles'
            'Get-ContentSourcesHash'
            'New-CatalogueCacheKey'
            'Read-CatalogueCacheEntry'
            'Read-CatalogueCacheStatus')) {

        . "$libRoot\catalogue-cache\$name.ps1"
    }

    . "$libRoot\content-catalogue\definitions\Get-ContentDefinitions.ps1"
    . "$libRoot\content-catalogue\readers\New-ParsedFileCache.ps1"
}

Describe 'Read-CatalogueCacheStatus' {

    BeforeEach {

        # Variant is built from Hull; each kind reads one file named for it, and Hull one more both share.
        Mock Get-ContentDefinitions {
            [pscustomobject]@{
                Kind = 'Hull';
                Generated = @() }
            [pscustomobject]@{
                Kind = 'Weapon';
                Generated = @() }
            [pscustomobject]@{
                Kind = 'Variant';
                Generated = @([pscustomobject]@{ FromKind = 'Hull' }) }
        }

        Mock Get-ContentKindInputFiles {

            "C:\$($Definition.Kind).file"

            if ($Definition.Kind -eq 'Hull') {
                'C:\Weapon.file'
            }
        }

        Mock Get-CatalogueCodeHash { 'code' }
        Mock Get-ContentSourcesHash { 'sources' }
        Mock New-ParsedFileCache { 'run cache' }
        Mock New-CatalogueCacheKey { "key of $($DataFilePaths -join '+')" }

        # Every kind is cached under the key it has now, until a case says otherwise.
        Mock Read-CatalogueCacheEntry { [pscustomobject]@{
            Key = 'current';
            GeneratedCount = 4 } }

        Mock Find-StaleCacheKeyPart { $null }
    }

    It 'keys each kind on its own files and those of the kinds it is built from, counting each file once' {

        # ARRANGE
        $expectedKeys = @(
            'key of C:\Hull.file+C:\Weapon.file',
            'key of C:\Variant.file+C:\Hull.file+C:\Weapon.file')

        $expectedDataFileCount = 3
        $expectedHullListingCount = 1

        # ACT
        $actualStatus = Read-CatalogueCacheStatus `
            -CacheFolder 'C:\cache' `
            -Sources @('sources') `
            -Kinds @('Hull', 'Variant')

        # ASSERT
        $actualStatus.
            Statuses.
            Key |
            Should -Be $expectedKeys

        $actualStatus.
            DataFileCount |
            Should -Be $expectedDataFileCount

        Should -Invoke Get-ContentKindInputFiles `
            -ParameterFilter { $Definition.Kind -eq 'Hull' } `
            -Times $expectedHullListingCount `
            -Exactly

        Should -Invoke Get-ContentKindInputFiles `
            -ParameterFilter { "$Sources" -eq 'sources' `
                -and $Cache -eq 'run cache' } `
            -Times 2 `
            -Exactly

        Should -Invoke New-CatalogueCacheKey `
            -ParameterFilter { $CodeHash -eq 'code' `
                -and $SourcesHash -eq 'sources' } `
            -Times 2 `
            -Exactly
    }

    It 'keeps the entry of a kind whose key is current' {

        # ARRANGE
        $expectedGeneratedCount = 4

        # ACT
        $actualStatus = Read-CatalogueCacheStatus `
            -CacheFolder 'C:\cache' `
            -Sources @() `
            -Kinds @('Weapon')

        # ASSERT
        $actualStatus.
            Statuses[0].
            RebuildReason |
            Should -BeNullOrEmpty

        $actualStatus.
            Statuses[0].
            CachedEntry.
            GeneratedCount |
            Should -Be $expectedGeneratedCount

        Should -Invoke Read-CatalogueCacheEntry `
            -ParameterFilter { $CacheFolder -eq 'C:\cache' `
                -and $Kind -eq 'Weapon' }

        Should -Invoke Find-StaleCacheKeyPart -ParameterFilter {
            $CachedKey -eq 'current' `
                -and $CurrentKey -eq 'key of C:\Weapon.file' }
    }

    It 'gives the reason for a kind whose key part changed, keeping no entry' {

        # ARRANGE
        Mock Find-StaleCacheKeyPart { 'the mods' }

        $expectedReason = 'the mods changed'

        # ACT
        $actualStatus = Read-CatalogueCacheStatus `
            -CacheFolder 'C:\cache' `
            -Sources @() `
            -Kinds @('Weapon')

        # ASSERT
        $actualStatus.
            Statuses[0].
            RebuildReason |
            Should -Be $expectedReason

        $actualStatus.
            Statuses[0].
            CachedEntry |
            Should -BeNullOrEmpty
    }

    It 'gives the reason for a kind with no entry' {

        # ARRANGE
        Mock Read-CatalogueCacheEntry { $null }

        $expectedReason = 'not cached yet'

        # ACT
        $actualStatus = Read-CatalogueCacheStatus `
            -CacheFolder 'C:\cache' `
            -Sources @() `
            -Kinds @('Weapon')

        # ASSERT
        $actualStatus.
            Statuses[0].
            RebuildReason |
            Should -Be $expectedReason

        Should -Invoke Find-StaleCacheKeyPart `
            -Times 0 `
            -Exactly
    }

    # A broken entry is read again rather than failing the run: the files hold the truth.
    It 'gives the reason for a kind whose entry does not parse' {

        # ARRANGE
        Mock Read-CatalogueCacheEntry { throw 'bad JSON' }

        $expectedReason = 'its cache entry does not parse: bad JSON'

        # ACT
        $actualStatus = Read-CatalogueCacheStatus `
            -CacheFolder 'C:\cache' `
            -Sources @() `
            -Kinds @('Weapon')

        # ASSERT
        $actualStatus.
            Statuses[0].
            RebuildReason |
            Should -Be $expectedReason
    }

    It 'rebuilds every kind when asked, without reading the cache' {

        # ARRANGE
        $expectedReasons = @(
            'asked to rebuild',
            'asked to rebuild')

        # ACT
        $actualStatus = Read-CatalogueCacheStatus `
            -CacheFolder 'C:\cache' `
            -Sources @() `
            -Kinds @('Hull', 'Weapon') `
            -IsRebuildForced

        # ASSERT
        $actualStatus.
            Statuses.
            RebuildReason |
            Should -Be $expectedReasons

        Should -Invoke Read-CatalogueCacheEntry `
            -Times 0 `
            -Exactly
    }
}
