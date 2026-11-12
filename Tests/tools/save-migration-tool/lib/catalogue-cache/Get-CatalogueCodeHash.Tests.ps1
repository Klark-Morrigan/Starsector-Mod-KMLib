BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\catalogue-cache\Get-CatalogueCodeHash.ps1"
    . "$libRoot\catalogue-cache\Get-TextHash.ps1"
}

Describe 'Get-CatalogueCodeHash' {

    BeforeEach {

        # Each catalogue folder lists one file holding a, except content, which lists z.ps1 before a.ps1.
        # The SHA-256 of a is CA97..., of b 3E23...
        Mock Get-ChildItem {

            $folderName = Split-Path -Leaf $LiteralPath
            $fileNames = if ($folderName -eq 'content') {
                @('z.ps1', 'a.ps1')
            } else {
                @('f.ps1')
            }

            foreach ($fileName in $fileNames) {

                $text = if ($fileName -eq 'z.ps1') {
                    'b'
                } else {
                    'a'
                }

                Get-Item -LiteralPath (New-TestFile -Name "$folderName\$fileName" -Text $text)
            }
        } -ParameterFilter { $File -and $Recurse }

        # The hash of the files is the text itself, so a case reads back which files went into it.
        Mock Get-TextHash { $Text }
    }

    # A file system lists files in its own order, so the lines are sorted by path before they are hashed.
    It 'hashes every file of the catalogue''s folders by its path and its content''s hash, in path order' {

        # ARRANGE
        $hashOfA = 'CA978112CA1BBDCAFAC231B39A23DC4DA786EFF8147C4E72B9807785AFEE48BB'
        $hashOfB = '3E23E8160039594A33894F6564E1B1348BBD7A0088D42C4ACB73EEAED59C009D'

        # A hyphen sorts before a slash, so content-catalogue comes before content.
        $expectedLinePatterns = @(
            "*/catalogue-cache/f.ps1 $hashOfA"
            "*/content-catalogue/f.ps1 $hashOfA"
            "*/content/a.ps1 $hashOfA"
            "*/content/z.ps1 $hashOfB"
            "*/data-file/f.ps1 $hashOfA"
            "*/install/f.ps1 $hashOfA"
        )

        # ACT
        $actualLines = @((Get-CatalogueCodeHash) -split "`n")

        # ASSERT
        $actualLines.Count | Should -Be $expectedLinePatterns.Count

        for ($index = 0; $index -lt $expectedLinePatterns.Count; $index++) {

            $actualLines[$index] |
                Should -BeLike $expectedLinePatterns[$index]
        }
    }
}
