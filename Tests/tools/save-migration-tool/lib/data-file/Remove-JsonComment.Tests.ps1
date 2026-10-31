BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Remove-JsonComment.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Remove-JsonLineComment.ps1"
}

Describe 'Remove-JsonComment' {

    BeforeEach {
        Mock Remove-JsonLineComment { "<stripped $Line>" }
    }

    It 'drops carriage returns from a text holding no #, reading no line on its own' {

        # ARRANGE
        $expectedText = "{`n`"a`": 1`n}"

        # ACT
        $actualText = Remove-JsonComment -Text "{`r`n`"a`": 1`r`n}"

        # ASSERT
        $actualText | Should -Be $expectedText
        Should -Invoke Remove-JsonLineComment -Times 0
    }

    It 'strips only the lines holding a #, dropping the carriage returns of the rest' {

        # ARRANGE
        $expectedText = "{`n<stripped # a comment`r>`n`"a`": 1`n}"

        # ACT
        $actualText = Remove-JsonComment -Text "{`r`n# a comment`r`n`"a`": 1`r`n}"

        # ASSERT
        $actualText | Should -Be $expectedText
        Should -Invoke Remove-JsonLineComment -Times 1 -Exactly
    }
}
