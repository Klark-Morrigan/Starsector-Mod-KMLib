BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\New-FailureReading.ps1"
}

Describe 'New-FailureReading' {

    # The names' order is the JSON's field order for every attempt and load.
    It 'returns the cause, missing content and object chain, empty, in that order' {

        # ARRANGE
        $expectedNames = @('CauseType', 'Cause', 'Missing', 'ObjectChain')

        # ACT
        $actualReading = New-FailureReading

        # ASSERT
        @($actualReading.Keys) | Should -Be $expectedNames
        $actualReading.CauseType | Should -BeNullOrEmpty
        $actualReading.Cause | Should -BeNullOrEmpty
        $actualReading.Missing | Should -BeNullOrEmpty
        $actualReading.ObjectChain.Count | Should -Be 0
    }

    # Each attempt is filled on its own, so one attempt's reading must never show through another's.
    It 'returns a new reading on each call' {

        # ARRANGE
        $filledReading = New-FailureReading
        $filledReading.Cause = 'filled'

        # ACT
        $actualReading = New-FailureReading

        # ASSERT
        $actualReading.Cause | Should -BeNullOrEmpty
    }
}
