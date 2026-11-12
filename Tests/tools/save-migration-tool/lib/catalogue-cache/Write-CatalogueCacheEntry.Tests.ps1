BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\Write-TextFile.ps1"
    . "$libRoot\catalogue-cache\Get-CatalogueCacheFileNames.ps1"
    . "$libRoot\catalogue-cache\Write-CatalogueCacheEntry.ps1"
}

Describe 'Write-CatalogueCacheEntry' {

    BeforeEach {

        $script:Writes = [System.Collections.Generic.List[object]]::new()

        Mock Get-CatalogueCacheFileNames {

            [pscustomobject]@{
                Folder = 'catalogue';
                Key = "$Kind.key.json";
                Owners = "$Kind.owners.json" }
        }

        Mock Write-TextFile { $script:Writes.Add([pscustomobject]@{
            Path = $Path;
            Text = $Text }) }
    }

    # A run stopped part-way leaves owners with no key, which a later run reads as no entry.
    It 'writes the owners, then the key with the generated count, in the catalogue folder' {

        # ARRANGE
        $cacheFolder = Join-Path $TestDrive 'cache'

        $key = [pscustomobject]@{
            Code = 'c';
            Mods = 'm';
            DataFiles = 'd' }

        $ownersById = New-OwnerIndex @{
            'a_hull' = @(
                'alpha',
                'vanilla') }

        $expectedPaths = @(
            "$cacheFolder\catalogue\Hull.owners.json",
            "$cacheFolder\catalogue\Hull.key.json")

        $expectedOwnersText = '{"a_hull":["alpha","vanilla"]}'
        $expectedCode = 'c'
        $expectedMods = 'm'
        $expectedDataFiles = 'd'
        $expectedGeneratedCount = 3

        # ACT
        Write-CatalogueCacheEntry `
            -CacheFolder $cacheFolder `
            -Kind 'Hull' `
            -Key $key `
            -OwnersById $ownersById `
            -GeneratedCount 3

        # ASSERT
        $actualKey = $script:Writes[1].Text |
            ConvertFrom-Json

        $script:Writes.Path |
            Should -Be $expectedPaths

        $script:Writes[0].Text |
            Should -BeExactly $expectedOwnersText

        $actualKey.Code |
            Should -Be $expectedCode

        $actualKey.Mods |
            Should -Be $expectedMods

        $actualKey.DataFiles |
            Should -Be $expectedDataFiles

        $actualKey.GeneratedCount |
            Should -Be $expectedGeneratedCount

        Test-Path `
            -LiteralPath "$cacheFolder\catalogue" `
            -PathType Container |
            Should -BeTrue
    }
}
