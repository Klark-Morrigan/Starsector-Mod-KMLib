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

        # ARRANGE
        $loads = @(
            [pscustomobject]@{ SaveFolder = 'save_A'; Outcome = 'Failed' }
            [pscustomobject]@{ SaveFolder = 'save_B'; Outcome = 'Loaded' }
        )

        $expectedLines = @(
            ''
            '<Path>save_A: <Failed>'
            '  details of save_A'
            ''
            '<Path>save_B: <Loaded>'
            '  details of save_B'
        )

        # ACT
        $actualLines = Format-LoadListText -Loads $loads

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'says the logs hold no load when there is none to list' {

        # ARRANGE
        $expectedLines = @('', 'The logs hold no load to list.')

        # ACT
        $actualLines = Format-LoadListText -Loads @()

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }
}
