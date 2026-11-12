BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\catalogue-cache\Get-CatalogueCacheFileNames.ps1"
    . "$libRoot\catalogue-cache\Read-CatalogueCacheOwners.ps1"
    . "$libRoot\content-catalogue\owner-index\New-ContentOwnerIndex.ps1"
}

Describe 'Read-CatalogueCacheOwners' {

    BeforeEach {

        Mock Get-CatalogueCacheFileNames {

            [pscustomobject]@{
                Folder = 'catalogue';
                Key = "$Kind.key.json";
                Owners = "$Kind.owners.json" }
        }

        Mock New-ContentOwnerIndex {
            [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]]::new()
        }
    }

    # IDs differing only in case are two IDs to the game, so the index keeps them apart.
    It 'reads the kind''s owners into an owner index, IDs matching case-sensitively' {

        # ARRANGE
        New-TestFile `
            -Name 'cache\catalogue\Hull.owners.json' `
            -Text ('{"a_hull":["alpha","vanilla"],"A_hull":["beta"]}') |
            Out-Null

        $expectedOwners = 'A_hull=beta a_hull=alpha+vanilla'
        $expectedType = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]]

        # ACT
        $actualOwners = Read-CatalogueCacheOwners `
            -CacheFolder (Join-Path $TestDrive 'cache') `
            -Kind 'Hull'

        # ASSERT
        $actualOwners.GetType() |
            Should -Be $expectedType

        Format-OwnerIndex -OwnersById $actualOwners |
            Should -Be $expectedOwners
    }
}
