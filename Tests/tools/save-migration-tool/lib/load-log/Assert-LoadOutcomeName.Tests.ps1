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
        { Assert-LoadOutcomeName -Outcome 'Failed' } | Should -Not -Throw
    }

    It 'fails naming the outcomes there are, comparing by case' {

        { Assert-LoadOutcomeName -Outcome 'failed' } |
            Should -Throw 'no load outcome failed; the outcomes are Loaded, Failed, Unfinished'
    }
}
