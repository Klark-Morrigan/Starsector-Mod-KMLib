BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\install\Get-CoreOwnerName.ps1"
    . "$libRoot\content-catalogue\readers\Get-ContentSources.ps1"
    . "$libRoot\content-catalogue\readers\New-ContentSource.ps1"
    . "$libRoot\install\Get-InstallPathNames.ps1"

    function script:New-Mod {
        # One roster entry, with only what the sources read.
        param(
            [string] $Id,
            [string] $Name,
            [string] $SortString,
            [string[]] $ReplacedPaths = @()
        )

        return [pscustomobject]@{
            Id            = $Id
            Name          = $Name
            Version       = "$Id-version"
            SortString    = $SortString
            ReplacedPaths = $ReplacedPaths
            Folder        = "C:\Games\mods\$Id"
        }
    }
}

Describe 'Get-ContentSources' {

    BeforeEach {

        Mock Get-CoreOwnerName { 'core' }
        Mock Get-InstallPathNames { [pscustomobject]@{ CoreFolder = 'core-folder' } }
    }

    It 'lists each mod''s folder and replaced paths, then the game''s own folder last' {

        # ARRANGE
        $roster = @(New-Mod -Id 'alpha' -Name 'Alpha' -ReplacedPaths @('data/hulls/ship_data.csv'))

        $expectedOwners = @('alpha', 'core')
        $expectedNames = @('Alpha', 'Starsector')
        $expectedVersions = @('alpha-version', $null)
        $expectedFolders = @('C:\Games\mods\alpha', 'C:\Games\core-folder')
        $expectedIsMods = @($true, $false)
        $expectedModReplacedPaths = @('data/hulls/ship_data.csv')
        $expectedCoreReplacedPathCount = 0

        # ACT
        $actualSources = Get-ContentSources -StarsectorRoot 'C:\Games' -Roster $roster

        # ASSERT
        $actualSources.Owner | Should -Be $expectedOwners
        $actualSources.Name | Should -Be $expectedNames
        $actualSources.Version | Should -Be $expectedVersions
        $actualSources.Folder | Should -Be $expectedFolders
        $actualSources.IsMod | Should -Be $expectedIsMods
        $actualSources[0].ReplacedPaths | Should -Be $expectedModReplacedPaths
        $actualSources[1].ReplacedPaths.Count | Should -Be $expectedCoreReplacedPathCount
    }

    # Upper case sorts before lower case code unit by code unit: Apple, Banana, Cherry, apple.
    It 'orders the mods by sort string, else name, else ID, comparing code units' {

        # ARRANGE
        $roster = @(
            (New-Mod -Id 'by_name_lower' -Name 'apple')
            (New-Mod -Id 'by_sort_string' -Name 'Zulu' -SortString 'Banana')
            (New-Mod -Id 'Cherry')
            (New-Mod -Id 'by_name_upper' -Name 'Apple')
        )

        $expectedOwners = @('by_name_upper', 'by_sort_string', 'Cherry', 'by_name_lower', 'core')

        # ACT
        $actualSources = Get-ContentSources -StarsectorRoot 'C:\Games' -Roster $roster

        # ASSERT
        $actualSources.Owner | Should -Be $expectedOwners
    }

    # The game's sort is stable, so the roster's order decides between mods that sort alike.
    It 'keeps the roster''s order for mods that sort alike' {

        # ARRANGE
        $roster = @(
            (New-Mod -Id 'second_in_roster_first' -Name 'Same')
            (New-Mod -Id 'first_in_roster_second' -Name 'Same')
        )

        $expectedOwners = @('second_in_roster_first', 'first_in_roster_second', 'core')

        # ACT
        $actualSources = Get-ContentSources -StarsectorRoot 'C:\Games' -Roster $roster

        # ASSERT
        $actualSources.Owner | Should -Be $expectedOwners
    }

    It 'lists only the game''s own folder with no mods' {

        # ARRANGE
        $expectedOwners = @('core')

        # ACT
        $actualSources = Get-ContentSources -StarsectorRoot 'C:\Games' -Roster @()

        # ASSERT
        $actualSources.Owner | Should -Be $expectedOwners
    }
}
