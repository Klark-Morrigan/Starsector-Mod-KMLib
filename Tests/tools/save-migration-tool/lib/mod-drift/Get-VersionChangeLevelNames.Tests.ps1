BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-VersionChangeLevelNames.ps1"
}

Describe 'Get-VersionChangeLevelNames' {

    It 'names the three levels a version change ranks at' {

        # ARRANGE
        $expectedMajor = 'Major'
        $expectedMinor = 'Minor'
        $expectedPatch = 'Patch'

        # ACT
        $actualLevels = Get-VersionChangeLevelNames

        # ASSERT
        $actualLevels.Major | Should -Be $expectedMajor
        $actualLevels.Minor | Should -Be $expectedMinor
        $actualLevels.Patch | Should -Be $expectedPatch
    }
}
