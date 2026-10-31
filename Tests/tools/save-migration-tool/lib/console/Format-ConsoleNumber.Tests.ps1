BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleNumber.ps1"
}

Describe 'Format-ConsoleNumber' {

    BeforeEach {
        Mock Format-ConsoleText { "<$Role>$Text" }
    }

    It 'paints a number in the number role' {

        # ARRANGE
        $expectedText = '<Number>378'

        # ACT
        $actualText = Format-ConsoleNumber 378

        # ASSERT
        $actualText | Should -Be $expectedText
    }
}
