BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Read-StarsectorText.ps1"
}

Describe 'Read-StarsectorText' {

    # Two bytes that a code page would read as two characters, and UTF-8 as one: e with an acute accent (U+00E9).
    It 'reads the file as UTF-8' {

        $path = New-TestFile -Name 'utf8.json' -Bytes ([byte[]] (0x22, 0xC3, 0xA9, 0x22))

        Read-StarsectorText -Path $path | Should -Be "`"$([char] 0x00E9)`""
    }

    # A caller refuses a byte order mark, as the game does, so it must still see one.
    It 'keeps a byte order mark as a character' {

        $path = New-TestFile -Name 'bom.json' -Bytes ([byte[]] (0xEF, 0xBB, 0xBF, 0x7B, 0x7D))

        Read-StarsectorText -Path $path | Should -Be "$([char] 0xFEFF){}"
    }

    It 'keeps carriage returns, which each reader drops as the game does for its kind of file' {

        $path = New-TestFile -Name 'crlf.csv' -Text "id`r`na`r`n"

        Read-StarsectorText -Path $path | Should -Be "id`r`na`r`n"
    }
}
