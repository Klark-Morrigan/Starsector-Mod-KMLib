BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Get-LoadOutcomeNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Format-LoadOutcome.ps1"
}

Describe 'Format-LoadOutcome' {

    BeforeEach {

        Mock Get-LoadOutcomeNames { [pscustomobject]@{ Loaded = 'L'; Failed = 'F'; Unfinished = 'U' } }
        Mock Format-ConsoleText { "<$Role>$Text" }
    }

    It 'paints a load that ended <Outcome> as <Role>' -ForEach @(
        @{ Outcome = 'L'; Role = 'Success' }
        @{ Outcome = 'F'; Role = 'Error' }
        @{ Outcome = 'U'; Role = 'Warning' }
    ) {
        Format-LoadOutcome -Outcome $Outcome | Should -Be "<$Role>$Outcome"
    }
}
