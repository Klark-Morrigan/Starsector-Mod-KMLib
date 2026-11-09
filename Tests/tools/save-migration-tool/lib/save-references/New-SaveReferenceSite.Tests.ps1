BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-references\New-SaveReferenceSite.ps1"
}

Describe 'New-SaveReferenceSite' {

    It 'keeps what describes the site' {

        # ARRANGE
        $expectedKind = 'Weapon'
        $expectedPath = @('CIStack', 'd')
        $expectedFilterAttribute = 't'
        $expectedFilterValues = @('WEAPONS')
        $expectedPosition = 0

        # ACT
        $actualSite = New-SaveReferenceSite `
            -Kind 'Weapon' `
            -Path @('CIStack', 'd') `
            -FilterAttribute 't' `
            -FilterValues @('WEAPONS')

        # ASSERT
        $actualSite.Kind |
            Should -Be $expectedKind

        $actualSite.Path |
            Should -Be $expectedPath

        $actualSite.FilterAttribute |
            Should -Be $expectedFilterAttribute

        $actualSite.FilterValues |
            Should -Be $expectedFilterValues

        $actualSite.Position |
            Should -Be $expectedPosition

        $actualSite.AreJsonKeysIds |
            Should -BeFalse
    }

    It 'names a text site by its path' {

        # ARRANGE
        $expectedName = 'knownShips/st'

        # ACT
        $actualSite = New-SaveReferenceSite `
            -Kind 'Hull' `
            -Path @('knownShips', 'st')

        # ASSERT
        $actualSite.Name |
            Should -Be $expectedName
    }

    It 'names the filter on the first element, the position on the last and the attribute after them' {

        # ARRANGE
        $expectedName = 'SpID[i=modspec,ship_bp]/x[2]@d'

        # ACT
        $actualSite = New-SaveReferenceSite `
            -Kind 'Hullmod' `
            -Path @('SpID', 'x') `
            -Attribute 'd' `
            -Position 2 `
            -FilterAttribute 'i' `
            -FilterValues @('modspec', 'ship_bp')

        # ASSERT
        $actualSite.Name |
            Should -Be $expectedName
    }

    It 'names the JSON keys or the JSON key a site reads' {

        # ARRANGE
        $expectedNames = @('stats/s{keys}', 'CCEnt/j1{f5}')

        # ACT
        $actualNames = @(

            (New-SaveReferenceSite `
                -Kind 'Skill' `
                -Path @('stats', 's') `
                -AreJsonKeysIds).
                Name

            (New-SaveReferenceSite `
                -Kind 'CustomEntity' `
                -Path @('CCEnt', 'j1') `
                -JsonKey 'f5').
                Name
        )

        # ASSERT
        $actualNames | Should -Be $expectedNames
    }

    It 'leaves the path it was given unchanged by the name' {

        # ARRANGE
        $path = @('wpn', 'e', 'st')
        $expectedPath = @('wpn', 'e', 'st')

        # ACT
        New-SaveReferenceSite `
            -Kind 'Weapon' `
            -Path $path `
            -Position 2 |
            Out-Null

        # ASSERT
        $path | Should -Be $expectedPath
    }
}
