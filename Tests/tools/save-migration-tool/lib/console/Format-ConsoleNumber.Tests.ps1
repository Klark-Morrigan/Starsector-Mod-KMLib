BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleNumber.ps1"
}

Describe 'Format-ConsoleNumber' {

    BeforeEach {
        Mock Format-ConsoleText { "<$Role>$Text" }
    }

    It 'paints a number in the number role' {
        Format-ConsoleNumber 378 | Should -Be '<Number>378'
    }
}
