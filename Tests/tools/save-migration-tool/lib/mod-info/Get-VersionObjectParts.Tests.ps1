BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-JsonPropertyValue.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-VersionObjectParts.ps1"
}

Describe 'Get-VersionObjectParts' {

    BeforeEach {
        Mock Get-JsonPropertyValue { if ($Name -eq 'patch') { $null } else { "$Name-value" } }
    }

    It 'returns major, minor and patch in order, a missing one as null' {

        $parts = Get-VersionObjectParts -Version ([pscustomobject]@{})

        $parts.Count | Should -Be 3
        $parts[0] | Should -Be 'major-value'
        $parts[1] | Should -Be 'minor-value'
        $parts[2] | Should -BeNullOrEmpty
    }
}
