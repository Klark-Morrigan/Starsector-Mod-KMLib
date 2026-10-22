BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Remove-JsonLineComment.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Remove-JsonComment.ps1"
}

Describe 'Remove-JsonComment' {

    BeforeEach {
        Mock Remove-JsonLineComment { "<stripped $Line>" }
    }

    It 'drops carriage returns from a text holding no #, reading no line on its own' {

        Remove-JsonComment -Text "{`r`n`"a`": 1`r`n}" | Should -Be "{`n`"a`": 1`n}"

        Should -Invoke Remove-JsonLineComment -Times 0
    }

    It 'strips only the lines holding a #, dropping the carriage returns of the rest' {

        Remove-JsonComment -Text "{`r`n# a comment`r`n`"a`": 1`r`n}" |
            Should -Be "{`n<stripped # a comment`r>`n`"a`": 1`n}"

        Should -Invoke Remove-JsonLineComment -Times 1 -Exactly
    }
}
