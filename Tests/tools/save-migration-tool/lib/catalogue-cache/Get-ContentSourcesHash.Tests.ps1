BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\catalogue-cache\Get-ContentSourcesHash.ps1"
    . "$libRoot\catalogue-cache\Get-TextHash.ps1"
}

Describe 'Get-ContentSourcesHash' {

    BeforeEach {

        # The hash of the sources is the text itself, so a case reads back what was hashed.
        Mock Get-TextHash { $Text }
    }

    It 'hashes each source''s owner, version, folder, mod flag and replaced paths, in search order' {

        # ARRANGE
        $sources = @(
            [pscustomobject]@{
                Owner = 'alpha'
                Version = '1.2'
                Folder = 'C:\mods\Alpha'
                IsMod = $true
                ReplacedPaths = @('data/a.csv', 'data/b.json')
            }
            [pscustomobject]@{
                Owner = 'vanilla'
                Version = $null
                Folder = 'C:\core'
                IsMod = $false
                ReplacedPaths = @()
            }
        )

        $expectedText = "alpha|1.2|C:\mods\Alpha|True|data/a.csv|data/b.json`nvanilla||C:\core|False|"

        # ACT
        $actualText = Get-ContentSourcesHash -Sources $sources

        # ASSERT
        $actualText |
            Should -BeExactly $expectedText
    }
}
