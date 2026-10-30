BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content-catalogue\definitions\New-ContentKind.ps1"
}

Describe 'New-ContentKind' {

    It 'holds the kind, its parts, and no generated IDs unless given rules' {

        # ARRANGE
        $expectedKind = 'Hull'
        $expectedParts = @('part one', 'part two')
        $expectedGeneratedCount = 0

        # ACT
        $actualKind = New-ContentKind -Kind 'Hull' -Parts @('part one', 'part two')

        # ASSERT
        $actualKind.Kind | Should -Be $expectedKind
        $actualKind.Parts | Should -Be $expectedParts
        $actualKind.Generated.Count | Should -Be $expectedGeneratedCount
    }

    It 'holds the rules it is given for generated IDs' {

        # ARRANGE
        $expectedGenerated = @('rule')

        # ACT
        $actualKind = New-ContentKind -Kind 'Hull' -Parts @() -Generated @('rule')

        # ASSERT
        $actualKind.Generated | Should -Be $expectedGenerated
    }
}
