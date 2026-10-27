BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\class-index\Get-CoreOwnerName.ps1"
    . "$libRoot\content-catalogue\Get-ContentSources.ps1"
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

        $sources = Get-ContentSources -StarsectorRoot 'C:\Games' -Roster @(
            New-Mod -Id 'alpha' -Name 'Alpha' -ReplacedPaths @('data/hulls/ship_data.csv')
        )

        $sources.Owner | Should -Be @('alpha', 'core')
        $sources.Name | Should -Be @('Alpha', 'Starsector')
        $sources.Version | Should -Be @('alpha-version', $null)
        $sources.Folder | Should -Be @('C:\Games\mods\alpha', 'C:\Games\core-folder')
        $sources.IsMod | Should -Be @($true, $false)
        $sources[0].ReplacedPaths | Should -Be @('data/hulls/ship_data.csv')
        $sources[1].ReplacedPaths.Count | Should -Be 0
    }

    It 'orders the mods by sort string, else name, else ID, comparing code units' {

        $sources = Get-ContentSources -StarsectorRoot 'C:\Games' -Roster @(
            (New-Mod -Id 'by_name_lower' -Name 'apple')
            (New-Mod -Id 'by_sort_string' -Name 'Zulu' -SortString 'Banana')
            (New-Mod -Id 'Cherry')
            (New-Mod -Id 'by_name_upper' -Name 'Apple')
        )

        # Upper case sorts before lower case code unit by code unit: Apple, Banana, Cherry, apple.
        $sources.Owner | Should -Be @('by_name_upper', 'by_sort_string', 'Cherry', 'by_name_lower', 'core')
    }

    It 'keeps the roster''s order for mods that sort alike' {

        $sources = Get-ContentSources -StarsectorRoot 'C:\Games' -Roster @(
            (New-Mod -Id 'second_in_roster_first' -Name 'Same')
            (New-Mod -Id 'first_in_roster_second' -Name 'Same')
        )

        $sources.Owner | Should -Be @('second_in_roster_first', 'first_in_roster_second', 'core')
    }

    It 'lists only the game''s own folder with no mods' {
        (Get-ContentSources -StarsectorRoot 'C:\Games' -Roster @()).Owner | Should -Be @('core')
    }
}
