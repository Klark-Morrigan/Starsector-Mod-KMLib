BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-references\Select-BrokenReferences.ps1"

    function script:New-Reference {
        # One reference, as Read-SaveReferences returns it.
        param(
            [string] $Kind,
            [string] $Id,
            [hashtable] $UseCountBySite)

        $useCount = 0

        foreach ($siteCount in $UseCountBySite.Values) {
            $useCount += $siteCount
        }

        return [pscustomobject]@{
            Kind = $Kind;
            Id = $Id;
            UseCount = $useCount;
            UseCountBySite = $UseCountBySite }
    }
}

Describe 'Select-BrokenReferences' {

    BeforeEach {
        $script:OwnersByKind = [ordered]@{
            Hull = New-OwnerIndex @{ 'a_hull' = @('alpha') }
            Weapon = New-OwnerIndex @{ 'a_gun' = @('alpha') }
        }
    }

    It 'keeps the IDs the kind lacks, kinds in the catalogue''s order, then IDs code unit by code unit' {

        # ARRANGE
        $references = @(
            New-Reference 'Weapon' 'z_gun' @{ 'wpn/e/st[2]' = 1 }
            New-Reference 'Hull' 'b_hull' @{ 'knownShips/st' = 2 }
            New-Reference 'Hull' 'a_hull' @{ 'knownShips/st' = 5 }
            New-Reference 'Hull' 'B_hull' @{ 'knownShips/st' = 1 }
            New-Reference 'Weapon' 'a_gun' @{ 'wpn/e/st[2]' = 3 }
        )

        $expectedRows = @(
            'Hull B_hull 1',
            'Hull b_hull 2',
            'Weapon z_gun 1')

        # ACT
        $actualRows = @(Select-BrokenReferences `
            -References $references `
            -OwnersByKind $script:OwnersByKind)

        # ASSERT
        @($actualRows |
            ForEach-Object { "$($_.Kind) $($_.Id) $($_.UseCount)" }) |
            Should -Be $expectedRows
    }

    # The game looks content up case-sensitively, so an ID differing only in case is not defined.
    It 'matches IDs case-sensitively' {

        # ARRANGE
        $references = @(New-Reference 'Hull' 'A_HULL' @{ 'knownShips/st' = 1 })

        $expectedIds = @('A_HULL')

        # ACT
        $actualRows = @(Select-BrokenReferences `
            -References $references `
            -OwnersByKind $script:OwnersByKind)

        # ASSERT
        $actualRows.Id | Should -Be $expectedIds
    }

    It 'lists each row''s sites with the busiest first, a tie by name' {

        # ARRANGE
        $references = @(
            New-Reference 'Hull' 'b_hull' @{ 'savedVariant@hId' = 4; 'knownShips/st' = 96; 'priorityShips/st' = 4 }
        )

        $expectedSites = @(
            'knownShips/st',
            'priorityShips/st',
            'savedVariant@hId')

        $expectedUses = @(96, 4, 4)

        # ACT
        $actualRows = @(Select-BrokenReferences `
            -References $references `
            -OwnersByKind $script:OwnersByKind)

        # ASSERT
        @($actualRows[0].Sites.Keys) |
            Should -Be $expectedSites

        @($actualRows[0].Sites.Values) |
            Should -Be $expectedUses
    }

    It 'returns nothing when every ID is defined' {

        # ARRANGE
        $references = @(New-Reference 'Hull' 'a_hull' @{ 'knownShips/st' = 1 })

        # ACT
        $actualRows = @(Select-BrokenReferences `
            -References $references `
            -OwnersByKind $script:OwnersByKind)

        # ASSERT
        $actualRows | Should -BeNullOrEmpty
    }
}
