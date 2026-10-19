BeforeAll {
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Remove-JsonComment.ps1"
}

Describe 'Remove-JsonComment' {

    It 'strips a whole-line # comment and a trailing // comment' {

        $text = "{`n  # a comment line`n  `"id`": `"a`", // trailing`n}"

        Remove-JsonComment -Text $text | Should -Be "{`n  `n  `"id`": `"a`", `n}"
    }

    It 'keeps // and # inside a string' {

        $text = '{ "url": "https://example.com/#top" }'

        Remove-JsonComment -Text $text | Should -Be $text
    }

    It 'keeps a # after an escaped quote inside a string' {

        $text = '{ "name": "The \"Quoted\" # Mod" }'

        Remove-JsonComment -Text $text | Should -Be $text
    }

    It 'treats a lone slash as text' {
        Remove-JsonComment -Text '{ "a": 1 / 2 }' | Should -Be '{ "a": 1 / 2 }'
    }

    It 'strips a comment that runs to the end of the text' {
        Remove-JsonComment -Text '{ } # last' | Should -Be '{ } '
    }
}
