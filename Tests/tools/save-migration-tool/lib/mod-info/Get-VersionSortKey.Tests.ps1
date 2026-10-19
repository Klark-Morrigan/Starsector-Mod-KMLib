BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-JsonPropertyValue.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-VersionObjectParts.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-VersionSortKey.ps1"
}

Describe 'Get-VersionSortKey' {

    BeforeEach {
        Mock Get-VersionObjectParts { ,@(2, $null, 3) }
    }

    It 'pads <Version> to <Key>' -ForEach @(
        @{ Version = '1.10.0'; Key = '000001.000010.000000.000000' }
        @{ Version = '91.DEV'; Key = '000091.000000.000000.000000' }
        @{ Version = '1.3.1b'; Key = '000001.000003.000001.000000' }
        @{ Version = '0.1.98-RC2'; Key = '000000.000001.000098.000000' }
    ) {
        Get-VersionSortKey -Version $Version | Should -Be $Key
    }

    It 'sorts 1.10.0 above 1.9.0' {
        (Get-VersionSortKey -Version '1.10.0') -gt (Get-VersionSortKey -Version '1.9.0') | Should -BeTrue
    }

    It 'reads a bare number as its digits' {
        Get-VersionSortKey -Version 1.5 | Should -Be '000001.000005.000000.000000'
    }

    It 'reads the object form through its parts, a missing part as zero' {

        Get-VersionSortKey -Version ([pscustomobject]@{ major = 2 }) | Should -Be '000002.000000.000003.000000'

        Should -Invoke Get-VersionObjectParts -Times 1 -Exactly
    }

    It 'reads no version as all zeros' {
        Get-VersionSortKey -Version $null | Should -Be '000000.000000.000000.000000'
    }
}
