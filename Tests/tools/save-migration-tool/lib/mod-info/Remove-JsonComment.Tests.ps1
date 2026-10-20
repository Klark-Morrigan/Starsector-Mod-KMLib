BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Remove-JsonComment.ps1"
}

# Each expectation is what the game's LoadingUtils comment strip leaves of the same text.
Describe 'Remove-JsonComment' {

    It 'strips a whole-line # comment and a trailing one' {

        $text = "{`n# a comment line`n`"id`": `"a`", # trailing`n}"

        Remove-JsonComment -Text $text | Should -Be "{`n`n`"id`": `"a`", `n}"
    }

    It 'keeps a # inside a string' {

        $text = '{ "url": "https://example.com/#top" }'

        Remove-JsonComment -Text $text | Should -Be $text
    }

    It 'keeps a # after an even number of escaped quotes, since every quote flips the string' {

        $text = '{ "name": "The \"Quoted\" # Mod" }'

        Remove-JsonComment -Text $text | Should -Be $text
    }

    It 'strips from a # after an odd number of escaped quotes, as the game does' {
        Remove-JsonComment -Text '{ "name": "say \"#1 twice" }' | Should -Be '{ "name": "say \"'
    }

    It 'keeps // and /* */, which are not comments to the game' {

        $text = '{ "a": 1 // not a comment' + "`n" + '/* nor this */ }'

        Remove-JsonComment -Text $text | Should -Be $text
    }

    It 'ends a string at a line break, so a quote left open on one line does not hide the next line''s comment' {
        Remove-JsonComment -Text "{ `"a`": `"open`n# comment`n}" | Should -Be "{ `"a`": `"open`n`n}"
    }

    It 'drops carriage returns, keeping line feeds' {
        Remove-JsonComment -Text "{`r`n}" | Should -Be "{`n}"
    }
}
