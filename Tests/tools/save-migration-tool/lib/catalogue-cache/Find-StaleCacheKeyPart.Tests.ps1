BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\catalogue-cache\Find-StaleCacheKeyPart.ps1"

    function script:New-Key {
        # A cache key, as New-CatalogueCacheKey builds it.
        param(
            [string] $Code,
            [string] $Mods,
            [string] $DataFiles)

        return [pscustomobject]@{ Code = $Code; Mods = $Mods; DataFiles = $DataFiles }
    }
}

Describe 'Find-StaleCacheKeyPart' {

    It 'returns nothing for a key that matches in every part' {

        # ARRANGE
        $cachedKey = New-Key 'c' 'm' 'd'
        $currentKey = New-Key 'c' 'm' 'd'

        # ACT
        $actualPart = Find-StaleCacheKeyPart `
            -CachedKey $cachedKey `
            -CurrentKey $currentKey

        # ASSERT
        $actualPart | Should -BeNullOrEmpty
    }

    It 'names the data files when only they differ' {

        # ARRANGE
        $cachedKey = New-Key 'c' 'm' 'd'
        $currentKey = New-Key 'c' 'm' 'D'

        $expectedPart = 'the data files'

        # ACT
        $actualPart = Find-StaleCacheKeyPart `
            -CachedKey $cachedKey `
            -CurrentKey $currentKey

        # ASSERT
        $actualPart | Should -Be $expectedPart
    }

    It 'names the mods before the data files' {

        # ARRANGE
        $cachedKey = New-Key 'c' 'm' 'd'
        $currentKey = New-Key 'c' 'M' 'D'

        $expectedPart = 'the mods'

        # ACT
        $actualPart = Find-StaleCacheKeyPart `
            -CachedKey $cachedKey `
            -CurrentKey $currentKey

        # ASSERT
        $actualPart | Should -Be $expectedPart
    }

    # A change to how the catalogue reads makes whatever else changed moot.
    It 'names the code first' {

        # ARRANGE
        $cachedKey = New-Key 'c' 'm' 'd'
        $currentKey = New-Key 'C' 'M' 'D'

        $expectedPart = "the catalogue's code"

        # ACT
        $actualPart = Find-StaleCacheKeyPart `
            -CachedKey $cachedKey `
            -CurrentKey $currentKey

        # ASSERT
        $actualPart | Should -Be $expectedPart
    }
}
