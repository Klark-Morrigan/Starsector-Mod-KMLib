BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\ConvertTo-JsonValueText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Get-JsonStringPattern.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Repair-JsonLeniency.ps1"
}

Describe 'Repair-JsonLeniency' {

    BeforeEach {

        # Angle brackets mark what the value reader wrote, and the text it was handed.
        Mock ConvertTo-JsonValueText { "<$Text>" }

        # A stand-in string pattern without escapes, which Get-JsonStringPattern's own suite covers.
        Mock Get-JsonStringPattern { '"[^"]*"|''[^'']*''' }
    }

    It 'writes the unquoted value of <Raw> as the value reader writes it, trimmed' -ForEach @(
        @{
            Raw = '{ "a": 0.5f }';
            Expected = '{ "a": <0.5f> }' }
        @{
            Raw = '{ "patch":a }';
            Expected = '{ "patch":<a> }' }
        @{
            Raw = '{ "a": two words  }';
            Expected = '{ "a": <two words>  }' }
        @{
            Raw = '{ "a": 12 }';
            Expected = '{ "a": <12> }' }
    ) {

        # ARRANGE
        $expectedText = $Expected

        # ACT
        $actualText = Repair-JsonLeniency -Text $Raw

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'writes every unquoted value of an array, one after another' {

        # ARRANGE
        $expectedText = '{ "a": [<1.0f>, <-2f>, <STARS>] }'

        # ACT
        $actualText = Repair-JsonLeniency -Text '{ "a": [1.0f, -2f, STARS] }'

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    # org.json skips and trims every character up to a space, control characters included.
    It 'skips the control characters around a value, as blanks' {

        # ARRANGE
        $expectedText = "[`u{1}<x>`t, <y>`u{1F}]"

        # ACT
        $actualText = Repair-JsonLeniency -Text "[`u{1}x`t, y`u{1F}]"

        # ASSERT
        $actualText |
            Should -Be $expectedText
    }

    # org.json ends the value at the tab, then fails on the text after it, so no value is read.
    It 'reads no value split by a control character' {

        # ARRANGE
        $text = "{ `"a`": two`twords }"
        $expectedText = $text
        $expectedReadCount = 0

        # ACT
        $actualText = Repair-JsonLeniency -Text $text

        # ASSERT
        $actualText |
            Should -Be $expectedText

        Should -Invoke ConvertTo-JsonValueText `
            -Times $expectedReadCount `
            -Exactly
    }

    # A no-break space is above a space, so org.json keeps it in the value where .NET's \s would trim it.
    It 'keeps a no-break space in the value' {

        # ARRANGE
        $expectedText = "{ `"a`": <`u{A0}x`u{A0}> }"

        # ACT
        $actualText = Repair-JsonLeniency -Text "{ `"a`": `u{A0}x`u{A0} }"

        # ASSERT
        $actualText |
            Should -Be $expectedText
    }

    It 'hands the value reader no string, key or bracket' {

        # ARRANGE
        $text = '{ "a": { "h": "x" }, ''b'': [], "c": [{}] }'
        $expectedText = $text
        $expectedReadCount = 0

        # ACT
        $actualText = Repair-JsonLeniency -Text $text

        # ASSERT
        $actualText |
            Should -Be $expectedText

        Should -Invoke ConvertTo-JsonValueText `
            -Times $expectedReadCount `
            -Exactly
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

    It 'takes ; between entries as a comma, and = or => between a key and its value as a colon, then reads the value' {

        # ARRANGE
        $expectedText = '{ "a" : <1>, "b" : <x> }'

        # ACT
        $actualText = Repair-JsonLeniency -Text '{ "a" = 1; "b" => x }'

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    # org.json reads one root value and ignores what follows it.
    It 'drops the text after the root value' {

        # ARRANGE
        $expectedText = '{ "a": { "b": [<1>] } }'

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
