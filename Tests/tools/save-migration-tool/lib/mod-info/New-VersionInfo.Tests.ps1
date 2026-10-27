BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\New-VersionInfo.ps1"
}

Describe 'New-VersionInfo' {

    It 'keeps the parts and the release-candidate flag as given' {

        $versionInfo = New-VersionInfo -Major '0.98' -Minor '8' -Patch '1' -MinorIsRC $true

        $versionInfo.Major | Should -Be '0.98'
        $versionInfo.Minor | Should -Be '8'
        $versionInfo.Patch | Should -Be '1'
        $versionInfo.MinorIsRC | Should -BeTrue
    }

    # The game's parts start out empty, so its comparison never meets a null; a null here would compare unlike ''.
    It 'turns a missing part into an empty string' {

        $versionInfo = New-VersionInfo -Major $null -MinorIsRC $false

        $versionInfo.Major | Should -BeExactly ''
        $versionInfo.Minor | Should -BeExactly ''
        $versionInfo.Patch | Should -BeExactly ''
    }
}
