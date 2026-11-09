BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\install\Get-InstallPathNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\install\Get-SaveFilePath.ps1"
}

Describe 'Get-SaveFilePath' {

    BeforeEach {
        Mock Get-InstallPathNames { [pscustomobject]@{ SavesFolder = 'saves' } }
    }

    It 'names the file in the save''s folder under saves' {

        # ARRANGE
        $expectedPath = 'C:\Games\Starsector\saves\save_A_1\campaign.xml'

        # ACT
        $actualPath = Get-SaveFilePath `
            -StarsectorRoot 'C:\Games\Starsector' `
            -SaveFolder 'save_A_1' `
            -FileName 'campaign.xml'

        # ASSERT
        $actualPath |
            Should -Be $expectedPath
    }
}
