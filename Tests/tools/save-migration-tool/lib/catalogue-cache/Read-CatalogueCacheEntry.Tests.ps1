BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\catalogue-cache\Get-CatalogueCacheFileNames.ps1"
    . "$libRoot\catalogue-cache\Read-CatalogueCacheEntry.ps1"
}

Describe 'Read-CatalogueCacheEntry' {

    BeforeEach {

        Mock Get-CatalogueCacheFileNames {

            [pscustomobject]@{
                Folder = 'catalogue';
                Key = "$Kind.key.json";
                Owners = "$Kind.owners.json" }
        }

        $script:CacheFolder = Join-Path $TestDrive 'cache'
    }

    It 'reads the key and the generated count of a kind''s entry' {

        # ARRANGE
        New-TestFile `
            -Name 'cache\catalogue\Hull.owners.json' `
            -Text '{}' |
            Out-Null

        New-TestFile `
            -Name 'cache\catalogue\Hull.key.json' `
            -Text ('{ "Code": "c", "Mods": "m", "DataFiles": "d", "GeneratedCount": 3 }') |
            Out-Null

        $expectedCode = 'c'
        $expectedMods = 'm'
        $expectedDataFiles = 'd'
        $expectedGeneratedCount = 3

        # ACT
        $actualEntry = Read-CatalogueCacheEntry `
            -CacheFolder $script:CacheFolder -Kind 'Hull'

        # ASSERT
        $actualEntry.Key.Code |
            Should -Be $expectedCode

        $actualEntry.Key.Mods |
            Should -Be $expectedMods

        $actualEntry.Key.DataFiles |
            Should -Be $expectedDataFiles

        $actualEntry.GeneratedCount |
            Should -Be $expectedGeneratedCount
    }

    It 'returns nothing for a kind with no entry' {

        # ARRANGE

        # ACT
        $actualEntry = Read-CatalogueCacheEntry `
            -CacheFolder $script:CacheFolder -Kind 'Weapon'

        # ASSERT
        $actualEntry | Should -BeNullOrEmpty
    }

    # The key is written last, but owners deleted by hand leave a key with nothing to serve.
    It 'returns nothing for a key whose owners file is gone' {

        # ARRANGE
        New-TestFile `
            -Name 'cache\catalogue\Skill.key.json' `
            -Text '{ "Code": "c" }' |
            Out-Null

        # ACT
        $actualEntry = Read-CatalogueCacheEntry `
            -CacheFolder $script:CacheFolder -Kind 'Skill'

        # ASSERT
        $actualEntry | Should -BeNullOrEmpty
    }

    It 'fails on a key that does not parse' {

        # ARRANGE
        New-TestFile `
            -Name 'cache\catalogue\Faction.owners.json' `
            -Text '{}' |
            Out-Null

        New-TestFile `
            -Name 'cache\catalogue\Faction.key.json' `
            -Text '{ "Code": ' |
            Out-Null

        # ACT
        $actualError = { Read-CatalogueCacheEntry `
            -CacheFolder $script:CacheFolder `
            -Kind 'Faction' } |
            Should -Throw -PassThru

        # ASSERT
        $actualError | Should -Not -BeNullOrEmpty
    }
}
