BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Add-DefaultDisplay.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Group-ContentByOwner.ps1"

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

        $groups = Group-ContentByOwner -Rows $script:Rows -Sources $script:Sources

        $groups.Owner | Should -Be @('alpha', 'vanilla')
        $groups.Name | Should -Be @('Alpha', 'Starsector')
        $groups.Version | Should -Be @('1.2', $null)
        $groups.IdCount | Should -Be @(3, 2)
    }

    It 'holds each owner''s IDs by kind, in the rows'' order, an ID several owners define under each' {

        $groups = Group-ContentByOwner -Rows $script:Rows -Sources $script:Sources

        @($groups[0].Ids.Keys) | Should -Be @('Hull', 'Variant')
        $groups[0].Ids['Hull'] | Should -Be @('alpha_hull', 'shared_hull')
        $groups[0].Ids['Variant'] | Should -Be @('alpha_hull_Hull')
        @($groups[1].Ids.Keys) | Should -Be @('Hull', 'Weapon')
        $groups[1].Ids['Hull'] | Should -Be @('shared_hull')
    }

    It 'prints an entry as its owner, version, name and ID count' {

        $groups = Group-ContentByOwner -Rows $script:Rows -Sources $script:Sources

        $groups[0].Display | Should -Be 'Owner Version Name IdCount'
    }

    It 'returns nothing for no rows' {
        @(Group-ContentByOwner -Rows @() -Sources $script:Sources).Count | Should -Be 0
    }
}
