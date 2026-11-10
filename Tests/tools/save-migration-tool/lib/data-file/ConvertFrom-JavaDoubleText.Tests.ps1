BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\ConvertFrom-JavaDoubleText.ps1"
}

Describe 'ConvertFrom-JavaDoubleText' {

    It 'reads the decimal <Raw> as <Expected>' -ForEach @(
        @{ Raw = '0.5f'; Expected = 0.5 }
        @{ Raw = '1.0F'; Expected = 1.0 }
        @{ Raw = '2.5d'; Expected = 2.5 }
        @{ Raw = '3D'; Expected = 3.0 }
        @{ Raw = '.5'; Expected = 0.5 }
        @{ Raw = '5.'; Expected = 5.0 }
        @{ Raw = '1.e3'; Expected = 1000.0 }
        @{ Raw = '-1.5e3'; Expected = -1500.0 }
        @{ Raw = '+2E-2'; Expected = 0.02 }
        @{ Raw = '12'; Expected = 12.0 }
    ) {

        # ARRANGE
        $expectedDouble = $Expected

        # ACT
        $actualDouble = ConvertFrom-JavaDoubleText -Text $Raw

        # ASSERT
        $actualDouble | Should -BeOfType [double]
        $actualDouble | Should -Be $expectedDouble
    }

    It 'reads the hexadecimal <Raw> as <Expected>' -ForEach @(
        @{ Raw = '0x1.8p1'; Expected = 3.0 }
        @{ Raw = '-0X1P-2'; Expected = -0.25 }
        @{ Raw = '0x.8p1'; Expected = 1.0 }
        @{ Raw = '+0x1Ep1f'; Expected = 60.0 }
        @{ Raw = '0x10.p0'; Expected = 16.0 }
    ) {

        # ARRANGE
        $expectedDouble = $Expected

        # ACT
        $actualDouble = ConvertFrom-JavaDoubleText -Text $Raw

        # ASSERT
        $actualDouble | Should -BeOfType [double]
        $actualDouble | Should -Be $expectedDouble
    }

    It 'reads <Raw>, too large for a double, as infinity' -ForEach @(
        @{ Raw = '1e999' }
        @{ Raw = '0x1p99999999999' }
    ) {

        # ARRANGE
        $expectedDouble = [double]::PositiveInfinity

        # ACT
        $actualDouble = ConvertFrom-JavaDoubleText -Text $Raw

        # ASSERT
        $actualDouble | Should -Be $expectedDouble
    }

    It 'reads <Raw> as negative zero' -ForEach @(
        @{ Raw = '-0.0' }
        @{ Raw = '-0x0p0' }
    ) {

        # ARRANGE
        $expectedDouble = 0.0
        $expectedIsNegative = $true

        # ACT
        $actualDouble = ConvertFrom-JavaDoubleText -Text $Raw

        # ASSERT
        $actualIsNegative = [double]::IsNegative($actualDouble)

        $actualDouble | Should -Be $expectedDouble
        $actualIsNegative | Should -Be $expectedIsNegative
    }

    It 'returns nothing for <Raw>, which Java refuses' -ForEach @(
        @{ Raw = '' }
        @{ Raw = '.' }
        @{ Raw = 'e5' }
        @{ Raw = '1e' }
        @{ Raw = '1.2.3' }
        @{ Raw = '1.0ff' }
        @{ Raw = '1,5' }
        @{ Raw = '0x1.8' }
        @{ Raw = '0x.p1' }
        @{ Raw = 'NaN' }
        @{ Raw = 'Infinity' }
    ) {

        # ACT
        $actualDouble = ConvertFrom-JavaDoubleText -Text $Raw

        # ASSERT
        $actualDouble | Should -BeNullOrEmpty
    }
}
