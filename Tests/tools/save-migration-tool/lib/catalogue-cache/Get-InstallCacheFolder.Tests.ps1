BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\catalogue-cache\Get-InstallCacheFolder.ps1"
    . "$libRoot\catalogue-cache\Get-TextHash.ps1"
}

Describe 'Get-InstallCacheFolder' {

    BeforeEach {

        $script:OriginalLocalAppData = $env:LOCALAPPDATA
        $env:LOCALAPPDATA = 'C:\Users\Someone\AppData\Local'

        Mock Get-TextHash { '0123456789abcdef0123456789abcdef' }
    }

    AfterEach {
        $env:LOCALAPPDATA = $script:OriginalLocalAppData
    }

    It 'names a folder under LOCALAPPDATA by the first 16 characters of the install path''s hash' {

        # ARRANGE
        $expectedFolder = 'C:\Users\Someone\AppData\Local\KMLib\save-migration-tool\cache\0123456789abcdef'

        # ACT
        $actualFolder = Get-InstallCacheFolder -StarsectorRoot 'C:\Games\Starsector'

        # ASSERT
        $actualFolder | Should -Be $expectedFolder
    }

    # Windows matches paths ignoring case, so one install has one folder however its path is written.
    It 'hashes the full install path in lower case, without a trailing separator' {

        # ARRANGE
        $expectedHashedText = 'c:\games\starsector'

        # ACT
        Get-InstallCacheFolder -StarsectorRoot 'C:\Games\Starsector\' | Out-Null

        # ASSERT
        Should -Invoke Get-TextHash `
            -ParameterFilter { $Text -ceq $expectedHashedText } `
            -Times 1 `
            -Exactly
    }

    It 'falls back to the system''s local application data folder when LOCALAPPDATA is not set' {

        # ARRANGE
        $env:LOCALAPPDATA = ''

        $expectedFolder = Join-Path ([Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)) (
            'KMLib\save-migration-tool\cache\0123456789abcdef')

        # ACT
        $actualFolder = Get-InstallCacheFolder `
            -StarsectorRoot 'C:\Games\Starsector'

        # ASSERT
        $actualFolder | Should -Be $expectedFolder
    }
}
