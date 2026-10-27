BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\ConvertTo-StraightQuotes.ps1"
}

Describe 'ConvertTo-StraightQuotes' {

    # Built from code points, so this file stays ASCII: left and right double quotes (U+201C U+201D),
    # left and right single quotes (U+2018 U+2019), and the replacement character (U+FFFD).
    It 'turns a run of curly double quotes into one double quote' {

        $text = "say $([char] 0x201C)$([char] 0x201C)hi$([char] 0x201D) now"

        ConvertTo-StraightQuotes -Text $text | Should -Be 'say "hi" now'
    }

    It 'turns a run of curly single quotes or replacement characters into one apostrophe' {

        $text = "it$([char] 0x2019)s$([char] 0x2018)$([char] 0xFFFD)x"

        ConvertTo-StraightQuotes -Text $text | Should -Be "it's'x"
    }

    It 'leaves straight quotes and other text as they are' {
        ConvertTo-StraightQuotes -Text 'a "b" ''c''' | Should -Be 'a "b" ''c'''
    }
}
