BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Get-LoadJsonDepth.ps1"
}

Describe 'Get-LoadJsonDepth' {

    # A shallower depth would cut each class's owners out of the JSON.
    It 'reaches a load''s attempts, their object chains and each link''s owners' {
        Get-LoadJsonDepth | Should -Be 6
    }
}
