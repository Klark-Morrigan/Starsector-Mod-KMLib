BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Get-JsonPropertyValue.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\Get-VersionObjectParts.ps1"
}

Describe 'Get-VersionObjectParts' {

    BeforeEach {
        Mock Get-JsonPropertyValue { if ($Name -eq 'patch') { $null } else { "$Name-value" } }
    }

    It 'returns major, minor and patch in order, a missing one as null' {

        # ARRANGE
        $expectedCount = 3
        $expectedMajor = 'major-value'
        $expectedMinor = 'minor-value'

        # ACT
        $actualParts = Get-VersionObjectParts -Version ([pscustomobject]@{})

        # ASSERT
        $actualParts.Count | Should -Be $expectedCount
        $actualParts[0] | Should -Be $expectedMajor
        $actualParts[1] | Should -Be $expectedMinor
        $actualParts[2] | Should -BeNullOrEmpty
    }
}
