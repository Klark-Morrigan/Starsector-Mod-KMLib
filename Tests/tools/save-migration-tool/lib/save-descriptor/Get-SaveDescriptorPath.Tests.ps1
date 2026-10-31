BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\install\Get-InstallPathNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-descriptor\Get-SaveDescriptorPath.ps1"
}

Describe 'Get-SaveDescriptorPath' {

    BeforeEach {
        Mock Get-InstallPathNames { [pscustomobject]@{ SavesFolder = 'saves'; DescriptorFile = 'descriptor.xml' } }
    }

    It 'names the descriptor in the save''s folder under saves' {

        # ARRANGE
        $expectedPath = 'C:\Games\Starsector\saves\save_A_1\descriptor.xml'

        # ACT
        $actualPath = Get-SaveDescriptorPath -StarsectorRoot 'C:\Games\Starsector' -SaveFolder 'save_A_1'

        # ASSERT
        $actualPath | Should -Be $expectedPath
    }
}
