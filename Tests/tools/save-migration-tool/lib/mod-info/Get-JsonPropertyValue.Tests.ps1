BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-JsonPropertyValue.ps1"
}

Describe 'Get-JsonPropertyValue' {

    It 'returns the value of a present property' {
        Get-JsonPropertyValue -Object ([pscustomobject]@{ id = 'alpha' }) -Name 'id' | Should -Be 'alpha'
    }

    It 'returns null for a missing property under strict mode' {

        Set-StrictMode -Version Latest

        Get-JsonPropertyValue -Object ([pscustomobject]@{ id = 'alpha' }) -Name 'version' | Should -BeNullOrEmpty
    }

    It 'returns null for no object' {
        Get-JsonPropertyValue -Object $null -Name 'id' | Should -BeNullOrEmpty
    }
}
