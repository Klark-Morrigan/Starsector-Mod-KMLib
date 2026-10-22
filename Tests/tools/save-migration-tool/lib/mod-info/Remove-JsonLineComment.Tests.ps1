BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Remove-JsonLineComment.ps1"
}

# Each expectation is what the game's LoadingUtils comment strip leaves of the same line.
Describe 'Remove-JsonLineComment' {

    It 'strips a whole-line # comment' {
        Remove-JsonLineComment -Line '# a comment line' | Should -Be ''
    }

    It 'strips a trailing # comment' {
        Remove-JsonLineComment -Line '"id": "a", # trailing' | Should -Be '"id": "a", '
    }

    It 'keeps a # inside a string' {

        $line = '{ "url": "https://example.com/#top" }'

        Remove-JsonLineComment -Line $line | Should -Be $line
    }

    It 'keeps a # after an even number of escaped quotes, since every quote flips the string' {

        $line = '{ "name": "The \"Quoted\" # Mod" }'

        Remove-JsonLineComment -Line $line | Should -Be $line
    }

    It 'strips from a # after an odd number of escaped quotes, as the game does' {
        Remove-JsonLineComment -Line '{ "name": "say \"#1 twice" }' | Should -Be '{ "name": "say \"'
    }

    It 'drops a carriage return, which ends a string as a line break does, so a # after it starts a comment' {
        Remove-JsonLineComment -Line "`"open # kept`rnext # gone" | Should -Be '"open # keptnext '
    }
}
