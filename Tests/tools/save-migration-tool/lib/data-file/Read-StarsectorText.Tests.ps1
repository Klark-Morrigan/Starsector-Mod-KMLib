BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Read-StarsectorText.ps1"
}

Describe 'Read-StarsectorText' {

    # Two bytes that a code page would read as two characters, and UTF-8 as one: e with an acute accent (U+00E9).
    It 'reads the file as UTF-8' {

        # ARRANGE
        $path = New-TestFile -Name 'utf8.json' -Bytes ([byte[]] (0x22, 0xC3, 0xA9, 0x22))
        $expectedText = "`"$([char] 0x00E9)`""

        # ACT
        $actualText = Read-StarsectorText -Path $path

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    # A caller refuses a byte order mark, as the game does, so it must still see one.
    It 'keeps a byte order mark as a character' {

        # ARRANGE
        $path = New-TestFile -Name 'bom.json' -Bytes ([byte[]] (0xEF, 0xBB, 0xBF, 0x7B, 0x7D))
        $expectedText = "$([char] 0xFEFF){}"

        # ACT
        $actualText = Read-StarsectorText -Path $path

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'keeps carriage returns, which each reader drops as the game does for its kind of file' {

        # ARRANGE
        $path = New-TestFile -Name 'crlf.csv' -Text "id`r`na`r`n"
        $expectedText = "id`r`na`r`n"

        # ACT
        $actualText = Read-StarsectorText -Path $path

        # ASSERT
        $actualText | Should -Be $expectedText
    }
}
