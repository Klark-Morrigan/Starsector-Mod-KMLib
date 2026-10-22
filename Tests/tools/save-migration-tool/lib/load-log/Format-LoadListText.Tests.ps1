BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\load-log\Format-LoadDetailLines.ps1"
    . "$libRoot\load-log\Format-LoadOutcome.ps1"
    . "$libRoot\load-log\Format-LoadListText.ps1"
}

Describe 'Format-LoadListText' {

    BeforeEach {

        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-LoadOutcome { "<$Outcome>" }
        Mock Format-LoadDetailLines { ,@("  details of $($Load.SaveFolder)") }
    }

    It 'writes each load under its save and outcome, each opening with a blank line, the first too' {

        $loads = @(
            [pscustomobject]@{ SaveFolder = 'save_A'; Outcome = 'Failed' }
            [pscustomobject]@{ SaveFolder = 'save_B'; Outcome = 'Loaded' }
        )

        Format-LoadListText -Loads $loads | Should -Be @(
            ''
            '<Path>save_A: <Failed>'
            '  details of save_A'
            ''
            '<Path>save_B: <Loaded>'
            '  details of save_B'
        )
    }

    It 'says the logs hold no load when there is none to list' {
        Format-LoadListText -Loads @() | Should -Be @('', 'The logs hold no load to list.')
    }
}
