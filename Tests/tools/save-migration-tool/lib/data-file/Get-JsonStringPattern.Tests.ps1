BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Get-JsonStringPattern.ps1"
}

# Each expectation is the first string org.json would read from the same text.
Describe 'Get-JsonStringPattern' {

    It 'matches <Description> whole' -ForEach @(
        @{
            Description = 'a double-quoted string';
            Text = '{ "url": "https://example.com/*x" }';
            Expected = '"url"' }
        @{
            Description = 'a single-quoted string holding a double quote';
            Text = '{ ''say "x'': 1 }';
            Expected = '''say "x''' }
        @{
            Description = 'a string holding an escaped quote';
            Text = '{ "say \"x\" twice": 1 }';
            Expected = '"say \"x\" twice"' }
        @{
            Description = 'a string ending in an escaped backslash';
            Text = '{ "a\\": "b" }';
            Expected = '"a\\"' }
    ) {

        # ARRANGE
        $expectedString = $Expected

        # ACT
        $actualString = [regex]::Match(
            $Text,
            (Get-JsonStringPattern)).
            Value

        # ASSERT
        $actualString |
            Should -BeExactly $expectedString
    }

    It 'matches no string left open' {

        # ARRANGE
        $expectedIsMatched = $false

        # ACT
        $actualIsMatched = [regex]::IsMatch(
            '{ "open: 1 }',
            (Get-JsonStringPattern))

        # ASSERT
        $actualIsMatched |
            Should -Be $expectedIsMatched
    }
}
