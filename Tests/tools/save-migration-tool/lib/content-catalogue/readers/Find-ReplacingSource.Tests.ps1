BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\readers\Find-ReplacingSource.ps1"

    function script:New-Source {
        # One data folder, with only what the finder reads.
        param(
            [string] $Owner,
            [string[]] $ReplacedPaths = @()
        )

        return [pscustomobject]@{ Owner = $Owner; ReplacedPaths = $ReplacedPaths }
    }
}

Describe 'Find-ReplacingSource' {

    # The game reads only the first replacing mod's copy, so a later one replacing the same path loses.
    It 'returns the first source in search order that replaces the path' {

        # ARRANGE
        $sources = @(
            (New-Source -Owner 'alpha')
            (New-Source -Owner 'beta' -ReplacedPaths @('data/other.json', 'data/sheet.csv'))
            (New-Source -Owner 'gamma' -ReplacedPaths @('data/sheet.csv'))
        )

        $expectedOwner = 'beta'

        # ACT
        $actualSource = Find-ReplacingSource -Sources $sources -Path 'data/sheet.csv'

        # ASSERT
        $actualSource.Owner | Should -Be $expectedOwner
    }

    It 'returns null when no source replaces the path' {

        # ARRANGE
        $sources = @(
            (New-Source -Owner 'alpha' -ReplacedPaths @('data/other.json'))
            (New-Source -Owner 'core')
        )

        # ACT
        $actualSource = Find-ReplacingSource -Sources $sources -Path 'data/sheet.csv'

        # ASSERT
        $actualSource | Should -BeNullOrEmpty
    }

    # The game takes a replaced JSON or content file's path written with either slash.
    It 'matches a replaced path written with <Description> when it accepts either slash' -ForEach @(
        @{ Description = 'forward slashes'; Replaced = 'data/hulls/a.ship' }
        @{ Description = 'backslashes'; Replaced = 'data\hulls\a.ship' }
    ) {

        # ARRANGE
        $sources = @(New-Source -Owner 'alpha' -ReplacedPaths @($Replaced))

        $expectedOwner = 'alpha'

        # ACT
        $actualSource = Find-ReplacingSource -Sources $sources -Path 'data/hulls/a.ship' -AcceptsEitherSlash

        # ASSERT
        $actualSource.Owner | Should -Be $expectedOwner
    }

    # The game compares a replaced spreadsheet's path as written, slashes included.
    It 'matches no replaced path written with backslashes unless it accepts either slash' {

        # ARRANGE
        $sources = @(New-Source -Owner 'alpha' -ReplacedPaths @('data\sheet.csv'))

        # ACT
        $actualSource = Find-ReplacingSource -Sources $sources -Path 'data/sheet.csv'

        # ASSERT
        $actualSource | Should -BeNullOrEmpty
    }

    # The game compares replaced paths case-sensitively, so a path differing in case replaces nothing.
    It 'matches a replaced path case-sensitively' {

        # ARRANGE
        $sources = @(New-Source -Owner 'alpha' -ReplacedPaths @('data/hulls/A.ship'))

        # ACT
        $actualSource = Find-ReplacingSource -Sources $sources -Path 'data/hulls/a.ship' -AcceptsEitherSlash

        # ASSERT
        $actualSource | Should -BeNullOrEmpty
    }
}
