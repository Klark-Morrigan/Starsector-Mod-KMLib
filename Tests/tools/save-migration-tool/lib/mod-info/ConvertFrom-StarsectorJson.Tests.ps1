BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Read-StarsectorText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Remove-JsonComment.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Repair-JsonLeniency.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Test-JsonSlashComment.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\ConvertFrom-StarsectorJson.ps1"
}

Describe 'ConvertFrom-StarsectorJson' {

    BeforeEach {

        Mock Remove-JsonComment { $Text }
        Mock Test-JsonSlashComment { $false }

        # The repair quotes a bare word "a"; anything else it leaves as it is.
        Mock Repair-JsonLeniency { $Text.Replace(':a', ':"a"') }
    }

    It 'parses strict JSON from a path' {

        # ARRANGE
        Mock Read-StarsectorText { '{ "id": "a" }' }

        $path = 'C:\mods\a\strict.json'
        $expectedId = 'a'

        # ACT
        $actualParsed = ConvertFrom-StarsectorJson -Path $path

        # ASSERT
        $actualParsed['id'] | Should -Be $expectedId
        Should -Invoke Read-StarsectorText -ParameterFilter { $Path -eq 'C:\mods\a\strict.json' } -Times 1 -Exactly
    }

    It 'parses text it is given' {

        # ARRANGE
        $expectedId = 'a'

        # ACT
        $actualParsed = ConvertFrom-StarsectorJson -Text '{ "id": "a" }'

        # ASSERT
        $actualParsed['id'] | Should -Be $expectedId
    }

    It 'hands the text to the comment stripper first' {

        # ARRANGE
        $text = '{ "id": "a" }'

        # ACT
        ConvertFrom-StarsectorJson -Text $text | Out-Null

        # ASSERT
        Should -Invoke Remove-JsonComment -Times 1 -Exactly
    }

    It 'takes the trailing commas and single-quoted strings org.json takes, with no repair' {

        # ARRANGE
        $text = "{ `"major`": '3', `"ids`": [`"a`", `"b`",], }"
        $expectedMajor = '3'
        $expectedIds = @('a', 'b')

        # ACT
        $actualParsed = ConvertFrom-StarsectorJson -Text $text

        # ASSERT
        $actualParsed['major'] | Should -Be $expectedMajor
        $actualParsed['ids'] | Should -Be $expectedIds
        Should -Invoke Repair-JsonLeniency -Times 0 -Exactly
    }

    It 'keeps a comma and brace inside a string as they are' {

        # ARRANGE
        $expectedValue = 'x,}'

        # ACT
        $actualParsed = ConvertFrom-StarsectorJson -Text '{ "a": "x,}" }'

        # ASSERT
        $actualParsed['a'] | Should -Be $expectedValue
    }

    It 'repairs what org.json takes and JSON refuses, when the text does not parse without it' {

        # ARRANGE
        $expectedPatch = 'a'

        # ACT
        $actualParsed = ConvertFrom-StarsectorJson -Text '{ "patch":a, "flag": true }'

        # ASSERT
        $actualParsed['patch'] | Should -Be $expectedPatch
        $actualParsed['flag'] | Should -BeTrue
        Should -Invoke Repair-JsonLeniency -Times 1 -Exactly
    }

    # A rewrite can corrupt a file that did not need one.
    It 'leaves text that parses as it is' {

        # ARRANGE
        $text = '{ "id": "a" }'

        # ACT
        ConvertFrom-StarsectorJson -Text $text | Out-Null

        # ASSERT
        Should -Invoke Repair-JsonLeniency -Times 0 -Exactly
    }

    # org.json keeps keys that differ only in case apart, and ConvertFrom-Json refuses them outside a hashtable.
    It 'returns objects, nested ones too, as hashtables keyed case-sensitively' {

        # ARRANGE
        $expectedKeys = @('a', 'A')
        $expectedNestedValue = 1

        # ACT
        $actualParsed = ConvertFrom-StarsectorJson -Text '{ "a": { "x": 1 }, "A": 2 }'

        # ASSERT
        @($actualParsed.Keys) | Should -Be $expectedKeys
        $actualParsed['a']['x'] | Should -Be $expectedNestedValue
        $actualParsed.ContainsKey('B') | Should -BeFalse
    }

    It 'rejects a // or /* */ comment, as the game does' {

        # ARRANGE
        Mock Test-JsonSlashComment { $true }

        $expectedMessage = 'holds a // or /* */ comment, which the game rejects: the given text'

        # ACT
        $actualError = { ConvertFrom-StarsectorJson -Text '{ "id": "a" }' } | Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -Be $expectedMessage
    }

    It 'rejects a file opening with a byte order mark, as the game does' {

        # ARRANGE
        Mock Read-StarsectorText { "$([char] 0xFEFF){}" }

        $expectedMessage = 'starts with a byte order mark, which the game rejects: C:\mods\a\bom.json'

        # ACT
        $actualError = { ConvertFrom-StarsectorJson -Path 'C:\mods\a\bom.json' } | Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -Be $expectedMessage
    }

    It 'fails naming the file when even the repair does not parse' {

        # ARRANGE
        Mock Read-StarsectorText { '{ "id": ' }

        $expectedMessage = "not valid JSON, even with org.json's leniencies repaired: C:\mods\a\broken.json *"

        # ACT
        $actualError = { ConvertFrom-StarsectorJson -Path 'C:\mods\a\broken.json' } | Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -BeLike $expectedMessage
    }
}
