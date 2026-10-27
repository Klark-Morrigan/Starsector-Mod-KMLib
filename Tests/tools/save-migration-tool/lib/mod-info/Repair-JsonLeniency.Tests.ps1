BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Repair-JsonLeniency.ps1"
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
        Repair-JsonLeniency -Text $Raw | Should -Be $Expected
    }

    It 'quotes every unquoted value of an array, one after another' {
        Repair-JsonLeniency -Text '{ "a": [1.0f, -2f, STARS] }' | Should -Be '{ "a": ["1.0f", "-2f", "STARS"] }'
    }

    It 'leaves the values JSON already takes as they are' {

        $text = '{ "a": 12, "b": -1.5e3, "c": true, "d": false, "e": null, "f": [0, 1], "g": { "h": "x" } }'

        Repair-JsonLeniency -Text $text | Should -Be $text
    }

    It 'leaves anything inside a quoted string as it is, single-quoted too' {

        $text = "{ `"a`": `"x, y]; z = w`", 'b': 'v, w]' }"

        Repair-JsonLeniency -Text $text | Should -Be $text
    }

    It 'takes ; between entries as a comma, and = or => between a key and its value as a colon' {
        Repair-JsonLeniency -Text '{ "a" = 1; "b" => 2 }' | Should -Be '{ "a" : 1, "b" : 2 }'
    }

    # org.json reads one root value and ignores what follows it.
    It 'drops the text after the root value' {
        Repair-JsonLeniency -Text '{ "a": { "b": [1] } }, }' | Should -Be '{ "a": { "b": [1] } }'
    }

    It 'counts no bracket inside a string toward the root''s end' {
        Repair-JsonLeniency -Text '{ "a": "}" } extra' | Should -Be '{ "a": "}" }'
    }
}
