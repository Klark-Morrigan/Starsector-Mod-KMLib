BeforeAll {
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Get-MissingContent.ps1"
}

Describe 'Get-MissingContent' {

    It 'reads <Cause> as <Kind> <Id>' -ForEach @(
        @{ Cause = 'Weapon spec [gone_gun] not found!'; Kind = 'Weapon'; Id = 'gone_gun' }
        @{ Cause = 'Ship hull spec [gone_hull] not found!'; Kind = 'Hull'; Id = 'gone_hull' }
        @{ Cause = 'Ship hull variant [gone_hull_Standard] not found!'; Kind = 'Variant'; Id = 'gone_hull_Standard' }
        @{ Cause = 'Fighter wing spec [gone_wing] not found'; Kind = 'FighterWing'; Id = 'gone_wing' }
        @{ Cause = 'Hullmod with id [gone_dmod] not found'; Kind = 'Hullmod'; Id = 'gone_dmod' }
    ) {

        $missing = Get-MissingContent -CauseType 'java.lang.RuntimeException' -Cause $Cause

        $missing.Kind | Should -Be $Kind
        $missing.Id | Should -Be $Id
        $missing.ReportedId | Should -Be $Id
        $missing.SpecClass | Should -BeNullOrEmpty
    }

    It 'reads a missing d-hull as its base hull, keeping the reported id' {

        $missing = Get-MissingContent -CauseType 'java.lang.RuntimeException' `
            -Cause 'Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'

        $missing.Kind | Should -Be 'Hull'
        $missing.Id | Should -Be 'BSC_Buffalo_Mk3'
        $missing.ReportedId | Should -Be 'BSC_Buffalo_Mk3_default_D'
    }

    It 'keeps a weapon id that ends like a d-hull as it is' {

        $missing = Get-MissingContent -CauseType 'java.lang.RuntimeException' `
            -Cause 'Weapon spec [gun_default_D] not found!'

        $missing.Id | Should -Be 'gun_default_D'
    }

    It 'reads a generic spec with its class' {

        $missing = Get-MissingContent -CauseType 'java.lang.RuntimeException' `
            -Cause 'Spec of class [com.fs.starfarer.loading.SpecialItemSpec] with id [gone_item] not found'

        $missing.Kind | Should -Be 'Spec'
        $missing.Id | Should -Be 'gone_item'
        $missing.SpecClass | Should -Be 'com.fs.starfarer.loading.SpecialItemSpec'
    }

    It 'reads an unresolved class as its name' {

        $missing = Get-MissingContent -CauseType 'com.thoughtworks.xstream.mapper.CannotResolveClassException' `
            -Cause 'data.scripts.removed.GoneScript'

        $missing.Kind | Should -Be 'Class'
        $missing.Id | Should -Be 'data.scripts.removed.GoneScript'
    }

    It 'returns null for <Description>' -ForEach @(
        @{ Description = 'a cause naming no content'; CauseType = 'java.lang.NullPointerException'; Cause = '' }
        @{ Description = 'a faction lookup, which is not a spec store'; CauseType = 'java.lang.RuntimeException'
            Cause = 'Hullmod with id [x] not found for faction [y]' }
        @{ Description = 'no cause at all'; CauseType = $null; Cause = $null }
    ) {
        Get-MissingContent -CauseType $CauseType -Cause $Cause | Should -BeNullOrEmpty
    }
}
