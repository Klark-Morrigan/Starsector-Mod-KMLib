BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Get-LoadOutcomeNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Assert-LoadOutcomeName.ps1"
}

Describe 'Assert-LoadOutcomeName' {

    BeforeEach {

        Mock Get-LoadOutcomeNames {
            [pscustomobject]@{ Loaded = 'Loaded'; Failed = 'Failed'; Unfinished = 'Unfinished' }
        }
    }

    It 'passes an outcome there is' {

        # ACT
        $actualCall = { Assert-LoadOutcomeName -Outcome 'Failed' }

        # ASSERT
        $actualCall | Should -Not -Throw
    }

    It 'fails naming the outcomes there are, comparing by case' {

        # ARRANGE
        $expectedMessage = 'no load outcome failed; the outcomes are Loaded, Failed, Unfinished'

        # ACT
        $actualError = { Assert-LoadOutcomeName -Outcome 'failed' } | Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -Be $expectedMessage
    }
}
