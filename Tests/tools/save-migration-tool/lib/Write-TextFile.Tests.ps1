BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Write-TextFile.ps1"
}

Describe 'Write-TextFile' {

    It 'writes the text, creating the file''s folder, and returns the full path' {

        # ARRANGE
        $path = Join-Path $TestDrive 'out\nested\file.json'

        $expectedPath = $path
        $expectedText = '{ "id": "alpha" }'

        # ACT
        $actualPath = Write-TextFile -Path $path -Text $expectedText

        # ASSERT
        $actualPath | Should -Be $expectedPath
        Get-Content -Raw -LiteralPath $path | Should -Be $expectedText
    }

    # A byte order mark ahead of JSON trips a reader that expects the first character to be the JSON's own.
    It 'writes UTF-8 without a byte order mark' {

        # ARRANGE
        $path = Join-Path $TestDrive 'bom.json'

        $expectedBytes = [byte[]] @(0x5B, 0x5D)

        # ACT
        Write-TextFile -Path $path -Text '[]' | Out-Null

        # ASSERT
        [System.IO.File]::ReadAllBytes($path) | Should -Be $expectedBytes
    }

    # .NET would resolve a relative path against the process's folder, which a shell's Set-Location never moves.
    It 'resolves a relative path against the shell''s current folder' {

        # ARRANGE
        $expectedPath = Join-Path $TestDrive 'relative\file.txt'

        Push-Location -LiteralPath $TestDrive

        # ACT
        try {
            $actualPath = Write-TextFile -Path 'relative\file.txt' -Text 'text'
        } finally {
            Pop-Location
        }

        # ASSERT
        $actualPath | Should -Be $expectedPath
        Test-Path -LiteralPath $expectedPath | Should -BeTrue
    }
}
