BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Remove-JsonComment.ps1"
}

# Each expectation is what the game's LoadingUtils leaves of the same text.
Describe 'Remove-JsonComment' {

    It 'drops the carriage returns of a text holding no #' {

        # ARRANGE
        $expectedText = "{`n`"a`": 1`n}"

        # ACT
        $actualText = Remove-JsonComment `
            -Text "{`r`n`"a`": 1`r`n}"

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }

    It 'strips a whole-line # comment' {

        # ARRANGE
        $expectedText = ''

        # ACT
        $actualText = Remove-JsonComment `
            -Text '# a comment line'

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }

    It 'strips a trailing # comment' {

        # ARRANGE
        $expectedText = '"id": "a", '

        # ACT
        $actualText = Remove-JsonComment `
            -Text '"id": "a", # trailing'

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }

    It 'strips the comment of every line, keeping the line feeds and dropping the carriage returns' {

        # ARRANGE
        $expectedText = "{`n`n`"a`": 1, `n}"

        # ACT
        $actualText = Remove-JsonComment `
            -Text "{`r`n# a comment`r`n`"a`": 1, # one`r`n}"

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }

    It 'keeps a # inside a string' {

        # ARRANGE
        $text = '{ "url": "https://example.com/#top" }'
        $expectedText = $text

        # ACT
        $actualText = Remove-JsonComment -Text $text

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }

    It 'keeps a # after an even number of escaped quotes, since every quote flips the string' {

        # ARRANGE
        $text = '{ "name": "The \"Quoted\" # Mod" }'
        $expectedText = $text

        # ACT
        $actualText = Remove-JsonComment -Text $text

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }

    It 'strips from a # after an odd number of escaped quotes, as the game does' {

        # ARRANGE
        $expectedText = '{ "name": "say \"'

        # ACT
        $actualText = Remove-JsonComment `
            -Text '{ "name": "say \"#1 twice" }'

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }

    It 'keeps a # inside a string left open at the end of the text' {

        # ARRANGE
        $text = '"open # kept'
        $expectedText = $text

        # ACT
        $actualText = Remove-JsonComment -Text $text

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }

    It 'ends an open string at a line feed, so a # on the next line starts a comment' {

        # ARRANGE
        $expectedText = "`"open # kept`nnext "

        # ACT
        $actualText = Remove-JsonComment `
            -Text "`"open # kept`nnext # gone"

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }

    It 'drops a carriage return before the strip, so it ends no open string' {

        # ARRANGE
        $expectedText = '"open # keptnext # gone'

        # ACT
        $actualText = Remove-JsonComment `
            -Text "`"open # kept`rnext # gone"

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }

    It 'strips a file of 2,000 commented lines in under 50 ms' {

        # ARRANGE
        $lineCount = 2000
        $timeLimitMilliseconds = 50

        $lines = for ($index = 0; $index -lt $lineCount; $index++) {
            "    `"key$index`": `"value # $index`", # comment $index"
        }

        $text = $lines -join "`r`n"
        $stopwatch = [System.Diagnostics.Stopwatch]::new()

        # ACT
        $stopwatch.Start()
        $null = Remove-JsonComment -Text $text
        $stopwatch.Stop()

        # ASSERT
        $stopwatch.
            Elapsed.
            TotalMilliseconds |
            Should -BeLessThan $timeLimitMilliseconds
    }
}
