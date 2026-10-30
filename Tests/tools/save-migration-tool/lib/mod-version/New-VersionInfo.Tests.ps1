BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\New-VersionInfo.ps1"
}

Describe 'New-VersionInfo' {

    It 'keeps the parts and the release-candidate flag as given' {

        # ARRANGE
        $expectedMajor = '0.98'
        $expectedMinor = '8'
        $expectedPatch = '1'
        $expectedMinorIsRC = $true

        # ACT
        $actualVersionInfo = New-VersionInfo -Major '0.98' -Minor '8' -Patch '1' -MinorIsRC $true

        # ASSERT
        $actualVersionInfo.Major | Should -Be $expectedMajor
        $actualVersionInfo.Minor | Should -Be $expectedMinor
        $actualVersionInfo.Patch | Should -Be $expectedPatch
        $actualVersionInfo.MinorIsRC | Should -Be $expectedMinorIsRC
    }

    # The game's parts start out empty, so its comparison never meets a null; a null here would compare unlike ''.
    It 'turns a missing part into an empty string' {

        # ARRANGE
        $expectedPart = ''

        # ACT
        $actualVersionInfo = New-VersionInfo -Major $null -MinorIsRC $false

        # ASSERT
        $actualVersionInfo.Major | Should -BeExactly $expectedPart
        $actualVersionInfo.Minor | Should -BeExactly $expectedPart
        $actualVersionInfo.Patch | Should -BeExactly $expectedPart
    }
}
