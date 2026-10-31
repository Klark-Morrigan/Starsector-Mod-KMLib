BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Repair-JsonLeniency.ps1"
}

Describe 'Repair-JsonLeniency' {

    # org.json keeps an unquoted value as a string unless it is a literal or a number Java parses.
    It 'quotes the unquoted value <Raw>' -ForEach @(
        @{ Raw = '{ "a": 0.5f }'; Expected = '{ "a": "0.5f" }' }
        @{ Raw = '{ "a": True }'; Expected = '{ "a": "True" }' }
        @{ Raw = '{ "patch":a }'; Expected = '{ "patch":"a" }' }
        @{ Raw = '{ "a": 049 }'; Expected = '{ "a": "049" }' }
        @{ Raw = '{ "a": +1 }'; Expected = '{ "a": "+1" }' }
        @{ Raw = '{ "a": two words }'; Expected = '{ "a": "two words" }' }
    ) {

        # ARRANGE
        $expectedText = $Expected

        # ACT
        $actualText = Repair-JsonLeniency -Text $Raw

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'quotes every unquoted value of an array, one after another' {

        # ARRANGE
        $expectedText = '{ "a": ["1.0f", "-2f", "STARS"] }'

        # ACT
        $actualText = Repair-JsonLeniency -Text '{ "a": [1.0f, -2f, STARS] }'

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'leaves the values JSON already takes as they are' {

        # ARRANGE
        $text = '{ "a": 12, "b": -1.5e3, "c": true, "d": false, "e": null, "f": [0, 1], "g": { "h": "x" } }'
        $expectedText = $text

        # ACT
        $actualText = Repair-JsonLeniency -Text $text

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'leaves anything inside a quoted string as it is, single-quoted too' {

        # ARRANGE
        $text = "{ `"a`": `"x, y]; z = w`", 'b': 'v, w]' }"
        $expectedText = $text

        # ACT
        $actualText = Repair-JsonLeniency -Text $text

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'takes ; between entries as a comma, and = or => between a key and its value as a colon' {

        # ARRANGE
        $expectedText = '{ "a" : 1, "b" : 2 }'

        # ACT
        $actualText = Repair-JsonLeniency -Text '{ "a" = 1; "b" => 2 }'

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    # org.json reads one root value and ignores what follows it.
    It 'drops the text after the root value' {

        # ARRANGE
        $expectedText = '{ "a": { "b": [1] } }'

        # ACT
        $actualText = Repair-JsonLeniency -Text '{ "a": { "b": [1] } }, }'

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'counts no bracket inside a string toward the root''s end' {

        # ARRANGE
        $expectedText = '{ "a": "}" }'

        # ACT
        $actualText = Repair-JsonLeniency -Text '{ "a": "}" } extra'

        # ASSERT
        $actualText | Should -Be $expectedText
    }
}
