BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Add-DefaultDisplay.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Group-ContentByOwner.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Get-ModIdComparer.ps1"

    function script:New-Row {
        # One catalogue row.
        param(
            [string] $Kind,
            [string] $Id,
            [string[]] $Owners
        )

        return [pscustomobject]@{ Kind = $Kind; Id = $Id; Owners = $Owners }
    }
}

Describe 'Group-ContentByOwner' {

    BeforeEach {

        Mock Add-DefaultDisplay {
            $InputObject | Add-Member -NotePropertyName 'Display' -NotePropertyValue "$PropertyNames" -PassThru
        }
        Mock Get-ModIdComparer { [System.StringComparer]::Ordinal }

        $script:Sources = @(
            [pscustomobject]@{ Owner = 'alpha'; Name = 'Alpha'; Version = '1.2' }
            [pscustomobject]@{ Owner = 'idle'; Name = 'Idle'; Version = '1.0' }
            [pscustomobject]@{ Owner = 'vanilla'; Name = 'Starsector'; Version = $null }
        )
        $script:Rows = @(
            (New-Row -Kind 'Hull' -Id 'alpha_hull' -Owners 'alpha')
            (New-Row -Kind 'Hull' -Id 'shared_hull' -Owners 'alpha', 'vanilla')
            (New-Row -Kind 'Weapon' -Id 'core_gun' -Owners 'vanilla')
            (New-Row -Kind 'Variant' -Id 'alpha_hull_Hull' -Owners 'alpha')
        )
    }

    It 'gives each owner that defines anything one entry, in the sources'' order, named as its source names it' {

        # ARRANGE
        $expectedOwners = @('alpha', 'vanilla')
        $expectedNames = @('Alpha', 'Starsector')
        $expectedVersions = @('1.2', $null)
        $expectedIdCounts = @(3, 2)

        # ACT
        $actualGroups = Group-ContentByOwner -Rows $script:Rows -Sources $script:Sources

        # ASSERT
        $actualGroups.Owner | Should -Be $expectedOwners
        $actualGroups.Name | Should -Be $expectedNames
        $actualGroups.Version | Should -Be $expectedVersions
        $actualGroups.IdCount | Should -Be $expectedIdCounts
    }

    It 'holds each owner''s IDs by kind, in the rows'' order, an ID several owners define under each' {

        # ARRANGE
        $expectedModKinds = @('Hull', 'Variant')
        $expectedModHulls = @('alpha_hull', 'shared_hull')
        $expectedModVariants = @('alpha_hull_Hull')
        $expectedCoreKinds = @('Hull', 'Weapon')
        $expectedCoreHulls = @('shared_hull')

        # ACT
        $actualGroups = Group-ContentByOwner -Rows $script:Rows -Sources $script:Sources

        # ASSERT
        @($actualGroups[0].Ids.Keys) | Should -Be $expectedModKinds
        $actualGroups[0].Ids['Hull'] | Should -Be $expectedModHulls
        $actualGroups[0].Ids['Variant'] | Should -Be $expectedModVariants
        @($actualGroups[1].Ids.Keys) | Should -Be $expectedCoreKinds
        $actualGroups[1].Ids['Hull'] | Should -Be $expectedCoreHulls
    }

    It 'prints an entry as its owner, version, name and ID count' {

        # ARRANGE
        $expectedDisplay = 'Owner Version Name IdCount'

        # ACT
        $actualGroups = Group-ContentByOwner -Rows $script:Rows -Sources $script:Sources

        # ASSERT
        $actualGroups[0].Display | Should -Be $expectedDisplay
    }

    # Owners are mod IDs, and the game keeps two IDs differing only in case apart.
    It 'keeps owners the mod ID comparer tells apart in separate entries' {

        # ARRANGE
        $sources = @(
            [pscustomobject]@{ Owner = 'alpha'; Name = 'Lower'; Version = '1.0' }
            [pscustomobject]@{ Owner = 'Alpha'; Name = 'Upper'; Version = '2.0' }
        )
        $rows = @(
            (New-Row -Kind 'Hull' -Id 'lower_hull' -Owners 'alpha')
            (New-Row -Kind 'Hull' -Id 'upper_hull' -Owners 'Alpha')
        )
        $expectedOwners = @('alpha', 'Alpha')
        $expectedIdCounts = @(1, 1)

        # ACT
        $actualGroups = Group-ContentByOwner -Rows $rows -Sources $sources

        # ASSERT
        $actualGroups.Owner | Should -BeExactly $expectedOwners
        $actualGroups.IdCount | Should -Be $expectedIdCounts
    }

    It 'returns nothing for no rows' {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualGroups = @(Group-ContentByOwner -Rows @() -Sources $script:Sources)

        # ASSERT
        $actualGroups.Count | Should -Be $expectedCount
    }
}
