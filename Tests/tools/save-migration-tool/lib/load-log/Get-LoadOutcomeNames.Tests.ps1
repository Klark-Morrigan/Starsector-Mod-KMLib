BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Get-LoadOutcomeNames.ps1"
}

Describe 'Get-LoadOutcomeNames' {

    It 'names the three outcomes a load attempt ends in' {

        # ARRANGE
        $expectedLoaded = 'Loaded'
        $expectedFailed = 'Failed'
        $expectedUnfinished = 'Unfinished'

        # ACT
        $actualOutcomes = Get-LoadOutcomeNames

        # ASSERT
        $actualOutcomes.Loaded | Should -Be $expectedLoaded
        $actualOutcomes.Failed | Should -Be $expectedFailed
        $actualOutcomes.Unfinished | Should -Be $expectedUnfinished
    }
}
