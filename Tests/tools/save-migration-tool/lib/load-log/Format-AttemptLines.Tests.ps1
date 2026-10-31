BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\load-log\Format-CauseLines.ps1"
    . "$libRoot\load-log\Format-LoadOutcome.ps1"
    . "$libRoot\load-log\Format-AttemptLines.ps1"
}

Describe 'Format-AttemptLines' {

    BeforeEach {

        # Marks each painted value with its role, so a case pins the colour of every value it names.
        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ConsoleNumber { "<Number>$Number" }
        Mock Format-LoadOutcome { "<$Outcome>" }
        Mock Format-CauseLines { @("<type $CauseType>", "<cause $Cause>") }
    }

    It 'nests under each attempt''s source how it ended, where it starts, and its cause when it has one' {

        # ARRANGE
        $attempts = @(
            [pscustomobject]@{
                Source = 'campaign'; Outcome = 'Failed'; LogFile = 'C:\logs\starsector.log.1'; LogLine = 12
                CauseType = 'T'; Cause = 'gone'
            }
            [pscustomobject]@{
                Source = 'campaign.bak'; Outcome = 'Loaded'; LogFile = 'C:\logs\starsector.log'; LogLine = 3
                CauseType = $null; Cause = $null
            }
        )

        $expectedLines = @(
            'Attempts, in order:'
            '  <Path>campaign:'
            '    <Failed> at <Path>starsector.log.1:<Number>12'
            '    <type T>'
            '    <cause gone>'
            '  <Path>campaign.bak:'
            '    <Loaded> at <Path>starsector.log:<Number>3'
        )

        # ACT
        $actualLines = Format-AttemptLines -Attempts $attempts

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'writes nothing for a load tried once' {

        # ARRANGE
        $attempts = @([pscustomobject]@{ Source = 'campaign'; Outcome = 'Failed'; CauseType = 'T'; Cause = 'gone' })

        $expectedCount = 0

        # ACT
        $actualLines = @(Format-AttemptLines -Attempts $attempts)

        # ASSERT
        $actualLines.Count | Should -Be $expectedCount
    }
}
