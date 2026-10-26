BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-ModDriftChangeNames.ps1"
}

Describe 'Get-ModDriftChangeNames' {

    It 'names the four ways a mod differs between a save and the install' {

        $changes = Get-ModDriftChangeNames

        $changes.Unchanged | Should -Be 'Unchanged'
        $changes.Changed | Should -Be 'Changed'
        $changes.Missing | Should -Be 'Missing'
        $changes.Added | Should -Be 'Added'
    }
}
