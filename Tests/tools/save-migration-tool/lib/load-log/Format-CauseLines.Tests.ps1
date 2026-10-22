BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Format-CauseLines.ps1"
}

Describe 'Format-CauseLines' {

    BeforeEach {
        Mock Format-ConsoleText { "<$Role>$Text" }
    }

    It 'writes the exception type as code, then the message as an error, a line each' {

        Format-CauseLines -CauseType 'java.lang.RuntimeException' -Cause 'Weapon spec [w] not found!' | Should -Be @(
            '<Code>java.lang.RuntimeException'
            '<Error>Weapon spec [w] not found!'
        )
    }
}
