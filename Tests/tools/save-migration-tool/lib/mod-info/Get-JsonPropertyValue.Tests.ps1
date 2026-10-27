BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\mod-info\Get-JsonPropertyValue.ps1"
    . "$libRoot\mod-info\Test-JsonProperty.ps1"
}

Describe 'Get-JsonPropertyValue' {

    It 'reads the value of a key the object holds' {

        # ARRANGE
        Mock Test-JsonProperty { $true }

        $object = [ordered]@{ id = 'a' }
        $expectedValue = 'a'

        # ACT
        $actualValue = Get-JsonPropertyValue -Object $object -Name 'id'

        # ASSERT
        $actualValue | Should -Be $expectedValue
    }

    It 'reads null for a key the object does not hold' {

        # ARRANGE
        Mock Test-JsonProperty { $false }

        $object = [ordered]@{ id = 'a' }
        $expectedValue = $null

        # ACT
        $actualValue = Get-JsonPropertyValue -Object $object -Name 'id'

        # ASSERT
        $actualValue | Should -Be $expectedValue
    }
}
