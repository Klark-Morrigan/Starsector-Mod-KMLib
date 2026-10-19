BeforeAll {
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Get-LoadOutcomeNames.ps1"
}

Describe 'Get-LoadOutcomeNames' {

    It 'names the three outcomes a load attempt ends in' {

        $outcomes = Get-LoadOutcomeNames

        $outcomes.Loaded | Should -Be 'Loaded'
        $outcomes.Failed | Should -Be 'Failed'
        $outcomes.Unfinished | Should -Be 'Unfinished'
    }
}
