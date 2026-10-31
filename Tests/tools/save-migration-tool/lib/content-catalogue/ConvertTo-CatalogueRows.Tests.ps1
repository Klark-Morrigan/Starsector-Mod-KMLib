BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\ConvertTo-CatalogueRows.ps1"

    function script:Format-CatalogueRows {
        # Writes each row as "<kind> <ID>: <owners>", in output order.
        param([object[]] $Rows)

        return @($Rows | ForEach-Object { "$($_.Kind) $($_.Id): $($_.Owners -join ', ')" })
    }
}

Describe 'ConvertTo-CatalogueRows' {

    BeforeEach {

        $script:OwnersByKind = [ordered]@{
            Weapon = New-OwnerIndex @{ beta_gun = @('beta'); Alpha_gun = @('alpha', 'vanilla') }
            Hull   = New-OwnerIndex @{ core_hull = @('vanilla') }
            Ping   = New-OwnerIndex
        }
    }

    # Code-unit order puts upper case first whatever the culture, so every machine lists the same order.
    It 'lists one row per ID, kinds in the catalogue''s order, IDs code unit by code unit, owners as indexed' {

        # ARRANGE
        $expectedRows = @(
            'Weapon Alpha_gun: alpha, vanilla'
            'Weapon beta_gun: beta'
            'Hull core_hull: vanilla'
        )

        # ACT
        $actualRows = ConvertTo-CatalogueRows -OwnersByKind $script:OwnersByKind

        # ASSERT
        Format-CatalogueRows -Rows $actualRows | Should -Be $expectedRows
    }

    # An array of owners stays a list in JSON, where the index's own list would not.
    It 'gives each row''s owners as an array' {

        # ARRANGE
        $expectedType = [string[]]

        # ACT
        $actualRows = ConvertTo-CatalogueRows -OwnersByKind $script:OwnersByKind

        # ASSERT
        , $actualRows[0].Owners | Should -BeOfType $expectedType
    }

    It 'lists only the IDs matching any of the patterns, ignoring case' {

        # ARRANGE
        $expectedRows = @(
            'Weapon Alpha_gun: alpha, vanilla'
            'Hull core_hull: vanilla'
        )

        # ACT
        $actualRows = ConvertTo-CatalogueRows -OwnersByKind $script:OwnersByKind -IdPatterns @('ALPHA_*', 'core_hull')

        # ASSERT
        Format-CatalogueRows -Rows $actualRows | Should -Be $expectedRows
    }

    It 'lists nothing when no ID matches' {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualRows = @(ConvertTo-CatalogueRows -OwnersByKind $script:OwnersByKind -IdPatterns @('missing_*'))

        # ASSERT
        $actualRows.Count | Should -Be $expectedCount
    }
}
