BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\install\Get-InstallPathNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\install\Get-SaveFilePath.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-descriptor\Get-SaveDescriptorPath.ps1"
}

Describe 'Get-SaveDescriptorPath' {

    BeforeEach {

        Mock Get-InstallPathNames { [pscustomobject]@{ DescriptorFile = 'descriptor.xml' } }
        Mock Get-SaveFilePath { "$StarsectorRoot|$SaveFolder|$FileName" }
    }

    It 'names the save''s descriptor file' {

        # ARRANGE
        $expectedPath = 'C:\Games\Starsector|save_A_1|descriptor.xml'

        # ACT
        $actualPath = Get-SaveDescriptorPath `
            -StarsectorRoot 'C:\Games\Starsector' `
            -SaveFolder 'save_A_1'

        # ASSERT
        $actualPath | Should -Be $expectedPath
    }
}
