BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Remove-JsonComment.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Test-JsonSlashComment.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\ConvertFrom-StarsectorJson.ps1"
}

Describe 'ConvertFrom-StarsectorJson' {

    BeforeEach {

        Mock Remove-JsonComment { $Text }
        Mock Test-JsonSlashComment { $false }
    }

    It 'parses strict JSON from a path' {
        (ConvertFrom-StarsectorJson -Path (New-TestFile -Name 'strict.json' -Text '{ "id": "a" }')).id | Should -Be 'a'
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

    It 'quotes a bare-word value, leaving true, false and null as literals' {

        $parsed = ConvertFrom-StarsectorJson -Text '{ "patch":a, "flag": true }'

        $parsed.patch | Should -Be 'a'
        $parsed.flag | Should -BeTrue
    }

    It 'rejects a // or /* */ comment, as the game does' {

        Mock Test-JsonSlashComment { $true }

        { ConvertFrom-StarsectorJson -Text '{ "id": "a" }' } |
            Should -Throw 'holds a // or /* */ comment, which the game rejects: the given text'
    }

    It 'rejects a file opening with a byte order mark, as the game does' {

        $bytes = [byte[]] (0xEF, 0xBB, 0xBF) + [System.Text.Encoding]::UTF8.GetBytes('{}')
        $path = New-TestFile -Name 'bom.json' -Bytes $bytes

        { ConvertFrom-StarsectorJson -Path $path } |
            Should -Throw "starts with a byte order mark, which the game rejects: $path"
    }

    It 'fails naming the file when even the repair does not parse' {

        $path = New-TestFile -Name 'broken.json' -Text '{ "id": '

        { ConvertFrom-StarsectorJson -Path $path } |
            Should -Throw "not valid JSON, even with unquoted values quoted: $path *"
    }
}
