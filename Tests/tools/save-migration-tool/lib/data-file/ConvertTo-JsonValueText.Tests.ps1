BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\ConvertFrom-JavaDoubleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\ConvertTo-JsonValueText.ps1"
}

Describe 'ConvertTo-JsonValueText' {

    BeforeEach {

        # Java refuses the text unless a case says otherwise.
        Mock ConvertFrom-JavaDoubleText { }
    }

    It 'writes <Raw> as the lowercase literal' -ForEach @(
        @{ Raw = 'True'; Expected = 'true' }
        @{ Raw = 'FALSE'; Expected = 'false' }
        @{ Raw = 'Null'; Expected = 'null' }
        @{ Raw = 'true'; Expected = 'true' }
    ) {

        # ARRANGE
        $expectedText = $Expected

        # ACT
        $actualText = ConvertTo-JsonValueText -Text $Raw

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'writes the hexadecimal <Raw> as the int <Expected>' -ForEach @(
        @{ Raw = '0x1F'; Expected = '31' }
        @{ Raw = '0X-1f'; Expected = '-31' }
        @{ Raw = '0x+10'; Expected = '16' }
        @{ Raw = '0x7FFFFFFF'; Expected = '2147483647' }
        @{ Raw = '0x-80000000'; Expected = '-2147483648' }
    ) {

        # ARRANGE
        $expectedText = $Expected

        # ACT
        $actualText = ConvertTo-JsonValueText -Text $Raw

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    # 0x1E holds an E, so it would be read as a double if the int did not come first.
    It 'reads a hexadecimal int holding an E as the int, not as a double' {

        # ARRANGE
        $expectedText = '30'
        $expectedDoubleReadCount = 0

        # ACT
        $actualText = ConvertTo-JsonValueText -Text '0x1E'

        # ASSERT
        $actualText |
            Should -Be $expectedText

        Should -Invoke ConvertFrom-JavaDoubleText `
            -Times $expectedDoubleReadCount `
            -Exactly
    }

    # Java's int parse fails, and with no . e or E the long parse fails on the x.
    It 'writes the hexadecimal <Raw>, which no int holds, as text' -ForEach @(
        @{ Raw = '0x80000000'; Expected = '"0x80000000"' }
        @{ Raw = '0x1G'; Expected = '"0x1G"' }
    ) {

        # ARRANGE
        $expectedText = $Expected

        # ACT
        $actualText = ConvertTo-JsonValueText -Text $Raw

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'writes <Raw> as the long <Expected>' -ForEach @(
        @{ Raw = '049'; Expected = '49' }
        @{ Raw = '+1'; Expected = '1' }
        @{ Raw = '-0'; Expected = '0' }
        @{ Raw = '12'; Expected = '12' }
        @{ Raw = '9223372036854775807'; Expected = '9223372036854775807' }
    ) {

        # ARRANGE
        $expectedText = $Expected

        # ACT
        $actualText = ConvertTo-JsonValueText -Text $Raw

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'writes <Raw>, which opens like a number but no long holds, as text' -ForEach @(
        @{ Raw = '9223372036854775808'; Expected = '"9223372036854775808"' }
        @{ Raw = '12f'; Expected = '"12f"' }
        @{ Raw = '-'; Expected = '"-"' }
    ) {

        # ARRANGE
        $expectedText = $Expected

        # ACT
        $actualText = ConvertTo-JsonValueText -Text $Raw

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'writes the double Java reads from <Raw> as <Expected>, kept a double' -ForEach @(
        @{ Raw = '0.5f'; Double = 0.5; Expected = '0.5' }
        @{ Raw = '1.0f'; Double = 1.0; Expected = '1.0' }
        @{ Raw = '-1.5e3'; Double = -1500.0; Expected = '-1500.0' }
        @{ Raw = '1e20'; Double = 1e20; Expected = '1E+20' }
        @{ Raw = '-0.0'; Double = -0.0; Expected = '-0.0' }
    ) {

        # ARRANGE
        $double = $Double
        Mock ConvertFrom-JavaDoubleText { $double }

        $expectedText = $Expected

        # ACT
        $actualText = ConvertTo-JsonValueText -Text $Raw

        # ASSERT
        $actualText |
            Should -Be $expectedText

        Should -Invoke ConvertFrom-JavaDoubleText `
            -ParameterFilter { $Text -ceq $Raw } `
            -Times 1 `
            -Exactly
    }

    It 'writes <Raw>, which holds . e or E but Java reads as no double, as text' -ForEach @(
        @{ Raw = '1.2.3'; Expected = '"1.2.3"' }
        @{ Raw = '.'; Expected = '"."' }
    ) {

        # ARRANGE
        $expectedText = $Expected

        # ACT
        $actualText = ConvertTo-JsonValueText -Text $Raw

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'fails on a double too large for its type, naming the value' {

        # ARRANGE
        Mock ConvertFrom-JavaDoubleText {
            [double]::PositiveInfinity }

        $expectedMessage = '1e999 is too large for a double, which the game rejects'

        # ACT
        $actualError = { ConvertTo-JsonValueText `
            -Text '1e999' } |
            Should -Throw -PassThru

        # ASSERT
        $actualError.
            Exception.
            Message |
            Should -Be $expectedMessage
    }

    It 'writes <Raw>, which opens with no digit, point or sign, as text' -ForEach @(
        @{ Raw = 'STARS'; Expected = '"STARS"' }
        @{ Raw = 'two words'; Expected = '"two words"' }
        @{ Raw = 'e5'; Expected = '"e5"' }
    ) {

        # ARRANGE
        $expectedText = $Expected
        $expectedDoubleReadCount = 0

        # ACT
        $actualText = ConvertTo-JsonValueText -Text $Raw

        # ASSERT
        $actualText |
            Should -Be $expectedText

        Should -Invoke ConvertFrom-JavaDoubleText `
            -Times $expectedDoubleReadCount `
            -Exactly
    }
}
