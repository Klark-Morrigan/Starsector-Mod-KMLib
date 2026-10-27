BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-VersionChangeLevelNames.ps1"
}

Describe 'Get-VersionChangeLevelNames' {

    It 'names the three levels a version change ranks at' {

        $levels = Get-VersionChangeLevelNames

        $levels.Major | Should -Be 'Major'
        $levels.Minor | Should -Be 'Minor'
        $levels.Patch | Should -Be 'Patch'
    }
}
