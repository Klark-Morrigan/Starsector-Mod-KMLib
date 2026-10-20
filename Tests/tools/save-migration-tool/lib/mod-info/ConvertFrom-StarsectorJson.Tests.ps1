BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Remove-JsonComment.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\ConvertFrom-StarsectorJson.ps1"

    function script:New-JsonFile {
        # Writes one file under TestDrive, as the given bytes when there are any, and returns its path.
        param(
            [string] $Name,
            [string] $Text,
            [byte[]] $Bytes
        )

        $path = Join-Path $TestDrive $Name

        if ($Bytes) {
            [System.IO.File]::WriteAllBytes($path, $Bytes)

        } else {
            [System.IO.File]::WriteAllText($path, $Text)
        }

        return $path
    }
}

Describe 'ConvertFrom-StarsectorJson' {

    BeforeEach {
        Mock Remove-JsonComment { $Text }
    }

    It 'parses strict JSON from a path' {
        (ConvertFrom-StarsectorJson -Path (New-JsonFile 'strict.json' '{ "id": "a" }')).id | Should -Be 'a'
    }

    It 'parses text it is given' {
        (ConvertFrom-StarsectorJson -Text '{ "id": "a" }').id | Should -Be 'a'
    }

    It 'hands the text to the comment stripper first' {

        ConvertFrom-StarsectorJson -Text '{ "id": "a" }' | Out-Null

        Should -Invoke Remove-JsonComment -Times 1 -Exactly
    }

    It 'drops trailing commas' {
        (ConvertFrom-StarsectorJson -Text '{ "ids": ["a", "b",], }').ids | Should -Be @('a', 'b')
    }

    It 'quotes single-quoted values' {
        (ConvertFrom-StarsectorJson -Text '{ "major": ''3'' }').major | Should -Be '3'
    }

    It 'quotes a bare-word value, leaving true, false and null as literals' {

        $parsed = ConvertFrom-StarsectorJson -Text '{ "patch":a, "flag": true }'

        $parsed.patch | Should -Be 'a'
        $parsed.flag | Should -BeTrue
    }

    It 'applies both repairs to a file that needs both' {

        $parsed = ConvertFrom-StarsectorJson -Text '{ "major": ''1'', "patch":a }'

        $parsed.major | Should -Be '1'
        $parsed.patch | Should -Be 'a'
    }

    It 'keeps prose apostrophes when the file parses without the single-quote repair' {

        $parsed = ConvertFrom-StarsectorJson -Text '{ "description": "the fleet''s, the crew''s" }'

        $parsed.description | Should -Be 'the fleet''s, the crew''s'
    }

    It 'rejects a file opening with a byte order mark, as the game does' {

        $bytes = [byte[]] (0xEF, 0xBB, 0xBF) + [System.Text.Encoding]::UTF8.GetBytes('{}')
        $path = New-JsonFile -Name 'bom.json' -Bytes $bytes

        { ConvertFrom-StarsectorJson -Path $path } |
            Should -Throw "starts with a byte order mark, which the game rejects: $path"
    }

    It 'fails naming the file when no repair parses' {

        $path = New-JsonFile 'broken.json' '{ "id": '

        { ConvertFrom-StarsectorJson -Path $path } | Should -Throw "no repair produced valid JSON: $path *"
    }
}
