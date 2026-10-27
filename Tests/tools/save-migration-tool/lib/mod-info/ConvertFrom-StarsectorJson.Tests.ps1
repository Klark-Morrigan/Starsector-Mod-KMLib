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

        Mock Read-StarsectorText { '{ "id": "a" }' }

        (ConvertFrom-StarsectorJson -Path 'C:\mods\a\strict.json').id | Should -Be 'a'

        Should -Invoke Read-StarsectorText -ParameterFilter { $Path -eq 'C:\mods\a\strict.json' } -Times 1 -Exactly
    }

    It 'parses text it is given' {
        (ConvertFrom-StarsectorJson -Text '{ "id": "a" }').id | Should -Be 'a'
    }

    It 'hands the text to the comment stripper first' {

        ConvertFrom-StarsectorJson -Text '{ "id": "a" }' | Out-Null

        Should -Invoke Remove-JsonComment -Times 1 -Exactly
    }

    It 'takes the trailing commas and single-quoted strings org.json takes, with no repair' {

        $parsed = ConvertFrom-StarsectorJson -Text "{ `"major`": '3', `"ids`": [`"a`", `"b`",], }"

        $parsed.major | Should -Be '3'
        $parsed.ids | Should -Be @('a', 'b')
    }

    It 'keeps a comma and brace inside a string as they are' {
        (ConvertFrom-StarsectorJson -Text '{ "a": "x,}" }').a | Should -Be 'x,}'
    }

    It 'repairs what org.json takes and JSON refuses, when the text does not parse without it' {

        $parsed = ConvertFrom-StarsectorJson -Text '{ "patch":a, "flag": true }'

        $parsed.patch | Should -Be 'a'
        $parsed.flag | Should -BeTrue

        Should -Invoke Repair-JsonLeniency -Times 1 -Exactly
    }

    # A rewrite can corrupt a file that did not need one.
    It 'leaves text that parses as it is' {

        ConvertFrom-StarsectorJson -Text '{ "id": "a" }' | Out-Null

        Should -Invoke Repair-JsonLeniency -Times 0 -Exactly
    }

    # org.json keeps keys that differ only in case apart, and ConvertFrom-Json refuses them outside a hashtable.
    It 'returns a hashtable keyed case-sensitively with -AsHashtable' {

        $parsed = ConvertFrom-StarsectorJson -Text '{ "a": { "x": 1 }, "A": 2 }' -AsHashtable

        @($parsed.Keys) | Should -Be @('a', 'A')
        $parsed['a']['x'] | Should -Be 1
        $parsed.ContainsKey('B') | Should -BeFalse
    }

    It 'rejects a // or /* */ comment, as the game does' {

        Mock Test-JsonSlashComment { $true }

        { ConvertFrom-StarsectorJson -Text '{ "id": "a" }' } |
            Should -Throw 'holds a // or /* */ comment, which the game rejects: the given text'
    }

    It 'rejects a file opening with a byte order mark, as the game does' {

        Mock Read-StarsectorText { "$([char] 0xFEFF){}" }

        { ConvertFrom-StarsectorJson -Path 'C:\mods\a\bom.json' } |
            Should -Throw 'starts with a byte order mark, which the game rejects: C:\mods\a\bom.json'
    }

    It 'fails naming the file when even the repair does not parse' {

        Mock Read-StarsectorText { '{ "id": ' }

        { ConvertFrom-StarsectorJson -Path 'C:\mods\a\broken.json' } |
            Should -Throw "not valid JSON, even with org.json's leniencies repaired: C:\mods\a\broken.json *"
    }
}
