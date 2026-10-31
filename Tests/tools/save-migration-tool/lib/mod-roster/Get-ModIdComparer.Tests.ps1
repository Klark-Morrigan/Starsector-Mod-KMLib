BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Get-ModIdComparer.ps1"
}

Describe 'Get-ModIdComparer' {

    # The game compares mod IDs with String.equals: Foo and foo are two mods.
    It 'tells apart IDs that differ only in case, as the game does' {

        # ARRANGE
        $expectedAcrossCase = $false
        $expectedSameCase = $true

        # ACT
        $actualComparer = Get-ModIdComparer

        # ASSERT
        $actualComparer.Equals('Foo', 'foo') | Should -Be $expectedAcrossCase
        $actualComparer.Equals('foo', 'foo') | Should -Be $expectedSameCase
    }
}
