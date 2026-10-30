BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content\Get-ContentKindNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Get-MissingContent.ps1"
}

Describe 'Get-MissingContent' {

    BeforeEach {

        Mock Get-ContentKindNames {
            [pscustomobject]@{
                Weapon      = 'kind-Weapon'
                Hull        = 'kind-Hull'
                Variant     = 'kind-Variant'
                FighterWing = 'kind-FighterWing'
                Hullmod     = 'kind-Hullmod'
                Spec        = 'kind-Spec'
                Class       = 'kind-Class'
            }
        }
    }

    It 'reads <Cause> as <Kind> <Id>' -ForEach @(
        @{ Cause = 'Weapon spec [gone_gun] not found!'; Kind = 'kind-Weapon'; Id = 'gone_gun' }
        @{ Cause = 'Ship hull spec [gone_hull] not found!'; Kind = 'kind-Hull'; Id = 'gone_hull' }
        @{ Cause = 'Ship hull variant [gone_Standard] not found!'; Kind = 'kind-Variant'; Id = 'gone_Standard' }
        @{ Cause = 'Fighter wing spec [gone_wing] not found'; Kind = 'kind-FighterWing'; Id = 'gone_wing' }
        @{ Cause = 'Hullmod with id [gone_dmod] not found'; Kind = 'kind-Hullmod'; Id = 'gone_dmod' }
    ) {

        # ARRANGE
        $expectedKind = $Kind
        $expectedId = $Id

        # ACT
        $actualMissing = Get-MissingContent -CauseType 'java.lang.RuntimeException' -Cause $Cause

        # ASSERT
        $actualMissing.Kind | Should -Be $expectedKind
        $actualMissing.Id | Should -Be $expectedId
        $actualMissing.ReportedId | Should -Be $expectedId
        $actualMissing.SpecClass | Should -BeNullOrEmpty
    }

    It 'reads a missing d-hull as its base hull, keeping the reported ID' {

        # ARRANGE
        $expectedKind = 'kind-Hull'
        $expectedId = 'BSC_Buffalo_Mk3'
        $expectedReportedId = 'BSC_Buffalo_Mk3_default_D'

        # ACT
        $actualMissing = Get-MissingContent -CauseType 'java.lang.RuntimeException' `
            -Cause 'Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'

        # ASSERT
        $actualMissing.Kind | Should -Be $expectedKind
        $actualMissing.Id | Should -Be $expectedId
        $actualMissing.ReportedId | Should -Be $expectedReportedId
    }

    It 'keeps a weapon ID that ends like a d-hull as it is' {

        # ARRANGE
        $expectedId = 'gun_default_D'

        # ACT
        $actualMissing = Get-MissingContent -CauseType 'java.lang.RuntimeException' `
            -Cause 'Weapon spec [gun_default_D] not found!'

        # ASSERT
        $actualMissing.Id | Should -Be $expectedId
    }

    It 'reads a generic spec with its class' {

        # ARRANGE
        $expectedKind = 'kind-Spec'
        $expectedId = 'gone_item'
        $expectedSpecClass = 'com.fs.starfarer.loading.SpecialItemSpec'

        # ACT
        $actualMissing = Get-MissingContent -CauseType 'java.lang.RuntimeException' `
            -Cause 'Spec of class [com.fs.starfarer.loading.SpecialItemSpec] with id [gone_item] not found'

        # ASSERT
        $actualMissing.Kind | Should -Be $expectedKind
        $actualMissing.Id | Should -Be $expectedId
        $actualMissing.SpecClass | Should -Be $expectedSpecClass
    }

    It 'reads an unresolved class as its name' {

        # ARRANGE
        $expectedKind = 'kind-Class'
        $expectedId = 'data.scripts.removed.GoneScript'

        # ACT
        $actualMissing = Get-MissingContent -CauseType 'com.thoughtworks.xstream.mapper.CannotResolveClassException' `
            -Cause 'data.scripts.removed.GoneScript'

        # ASSERT
        $actualMissing.Kind | Should -Be $expectedKind
        $actualMissing.Id | Should -Be $expectedId
    }

    It 'returns null for <Description>' -ForEach @(
        @{ Description = 'a cause naming no content'; CauseType = 'java.lang.NullPointerException'; Cause = '' }
        @{ Description = 'a faction lookup, which is not a spec store'; CauseType = 'java.lang.RuntimeException'
            Cause = 'Hullmod with id [x] not found for faction [y]' }
        @{ Description = 'no cause at all'; CauseType = $null; Cause = $null }
    ) {

        # ACT
        $actualMissing = Get-MissingContent -CauseType $CauseType -Cause $Cause

        # ASSERT
        $actualMissing | Should -BeNullOrEmpty
    }
}
