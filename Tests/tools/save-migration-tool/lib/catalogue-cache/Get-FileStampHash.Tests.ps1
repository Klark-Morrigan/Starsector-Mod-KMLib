BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\catalogue-cache\Get-FileStampHash.ps1"
    . "$libRoot\catalogue-cache\Get-TextHash.ps1"
}

Describe 'Get-FileStampHash' {

    BeforeEach {

        # The hash of the stamps is the text itself, so a case reads back what was stamped.
        Mock Get-TextHash { $Text }
    }

    It 'stamps each file with its path, length and last write time, in the order given' {

        # ARRANGE
        $firstPath = Join-Path $TestDrive 'b.csv'
        $secondPath = Join-Path $TestDrive 'a.ship'

        [System.IO.File]::WriteAllText($firstPath, 'abc')
        [System.IO.File]::WriteAllText($secondPath, 'hello')

        [System.IO.File]::SetLastWriteTimeUtc(
            $firstPath,
            [datetime]::new(2026, 1, 2, 0, 0, 0, [DateTimeKind]::Utc))

        [System.IO.File]::SetLastWriteTimeUtc(
            $secondPath,
            [datetime]::new(2026, 1, 3, 0, 0, 0, [DateTimeKind]::Utc))

        $expectedText = "$firstPath|3|639029088000000000`n$secondPath|5|639029952000000000"

        # ACT
        $actualText = Get-FileStampHash -Paths @($firstPath, $secondPath)

        # ASSERT
        $actualText | Should -BeExactly $expectedText
    }

    # A file gone between the listing and the stamp still changes the key from the run that saw it.
    It 'stamps a missing file as missing' {

        # ARRANGE
        $missingPath = Join-Path $TestDrive 'gone.ship'

        $expectedText = "$missingPath|missing"

        # ACT
        $actualText = Get-FileStampHash -Paths @($missingPath)

        # ASSERT
        $actualText | Should -BeExactly $expectedText
    }
}
