BeforeAll {

    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Remove-JsonComment.ps1"
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\ConvertFrom-StarsectorJson.ps1"

    function script:New-JsonFile {
        # Writes one file under TestDrive and returns its path.
        param(
            [string] $Name,
            [string] $Text
        )

        $path = Join-Path $TestDrive $Name

        [System.IO.File]::WriteAllText($path, $Text)

        return $path
    }
}

Describe 'ConvertFrom-StarsectorJson' {

    BeforeEach {
        Mock Remove-JsonComment { $Text }
    }

    It 'parses strict JSON as it is' {
        (ConvertFrom-StarsectorJson -Path (New-JsonFile 'strict.json' '{ "id": "a" }')).id | Should -Be 'a'
    }

    It 'hands the text to the comment stripper first' {

        ConvertFrom-StarsectorJson -Path (New-JsonFile 'stripped.json' '{ "id": "a" }') | Out-Null

        Should -Invoke Remove-JsonComment -Times 1 -Exactly
    }

    It 'drops trailing commas' {
        (ConvertFrom-StarsectorJson -Path (New-JsonFile 'trailing.json' '{ "ids": ["a", "b",], }')).ids |
            Should -Be @('a', 'b')
    }

    It 'quotes single-quoted values' {
        (ConvertFrom-StarsectorJson -Path (New-JsonFile 'single.json' '{ "major": ''3'' }')).major | Should -Be '3'
    }

    It 'quotes a bare-word value, leaving true, false and null as literals' {

        $parsed = ConvertFrom-StarsectorJson -Path (New-JsonFile 'bare.json' '{ "patch":a, "flag": true }')

        $parsed.patch | Should -Be 'a'
        $parsed.flag | Should -BeTrue
    }

    It 'keeps prose apostrophes when the file parses without the single-quote repair' {

        $path = New-JsonFile 'prose.json' '{ "description": "the fleet''s, the crew''s" }'
        $parsed = ConvertFrom-StarsectorJson -Path $path

        $parsed.description | Should -Be 'the fleet''s, the crew''s'
    }

    It 'fails naming the file when no repair parses' {

        $path = New-JsonFile 'broken.json' '{ "id": '

        { ConvertFrom-StarsectorJson -Path $path } | Should -Throw "no repair produced valid JSON: $path *"
    }
}
