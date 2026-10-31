BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-ModDriftChangeNames.ps1"
}

Describe 'Get-ModDriftChangeNames' {

    It 'names the four ways a mod differs between a save and the install' {

        # ARRANGE
        $expectedUnchanged = 'Unchanged'
        $expectedChanged = 'Changed'
        $expectedMissing = 'Missing'
        $expectedAdded = 'Added'

        # ACT
        $actualChanges = Get-ModDriftChangeNames

        # ASSERT
        $actualChanges.Unchanged | Should -Be $expectedUnchanged
        $actualChanges.Changed | Should -Be $expectedChanged
        $actualChanges.Missing | Should -Be $expectedMissing
        $actualChanges.Added | Should -Be $expectedAdded
    }
}
