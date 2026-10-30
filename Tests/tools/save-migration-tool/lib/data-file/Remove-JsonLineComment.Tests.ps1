BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Remove-JsonLineComment.ps1"
}

# Each expectation is what the game's LoadingUtils comment strip leaves of the same line.
Describe 'Remove-JsonLineComment' {

    It 'strips a whole-line # comment' {

        # ARRANGE
        $expectedLine = ''

        # ACT
        $actualLine = Remove-JsonLineComment -Line '# a comment line'

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }

    It 'strips a trailing # comment' {

        # ARRANGE
        $expectedLine = '"id": "a", '

        # ACT
        $actualLine = Remove-JsonLineComment -Line '"id": "a", # trailing'

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }

    It 'keeps a # inside a string' {

        # ARRANGE
        $line = '{ "url": "https://example.com/#top" }'
        $expectedLine = $line

        # ACT
        $actualLine = Remove-JsonLineComment -Line $line

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }

    It 'keeps a # after an even number of escaped quotes, since every quote flips the string' {

        # ARRANGE
        $line = '{ "name": "The \"Quoted\" # Mod" }'
        $expectedLine = $line

        # ACT
        $actualLine = Remove-JsonLineComment -Line $line

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }

    It 'strips from a # after an odd number of escaped quotes, as the game does' {

        # ARRANGE
        $expectedLine = '{ "name": "say \"'

        # ACT
        $actualLine = Remove-JsonLineComment -Line '{ "name": "say \"#1 twice" }'

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }

    It 'drops a carriage return, which ends a string as a line break does, so a # after it starts a comment' {

        # ARRANGE
        $expectedLine = '"open # keptnext '

        # ACT
        $actualLine = Remove-JsonLineComment -Line "`"open # kept`rnext # gone"

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }
}
