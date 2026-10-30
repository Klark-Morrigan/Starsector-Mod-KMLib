BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\ConvertTo-StraightQuotes.ps1"
}

Describe 'ConvertTo-StraightQuotes' {

    # Built from code points, so this file stays ASCII: left and right double quotes (U+201C U+201D),
    # left and right single quotes (U+2018 U+2019), and the replacement character (U+FFFD).
    It 'turns a run of curly double quotes into one double quote' {

        # ARRANGE
        $text = "say $([char] 0x201C)$([char] 0x201C)hi$([char] 0x201D) now"
        $expectedText = 'say "hi" now'

        # ACT
        $actualText = ConvertTo-StraightQuotes -Text $text

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'turns a run of curly single quotes or replacement characters into one apostrophe' {

        # ARRANGE
        $text = "it$([char] 0x2019)s$([char] 0x2018)$([char] 0xFFFD)x"
        $expectedText = "it's'x"

        # ACT
        $actualText = ConvertTo-StraightQuotes -Text $text

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'leaves straight quotes and other text as they are' {

        # ARRANGE
        $text = 'a "b" ''c'''
        $expectedText = $text

        # ACT
        $actualText = ConvertTo-StraightQuotes -Text $text

        # ASSERT
        $actualText | Should -Be $expectedText
    }
}
