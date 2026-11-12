BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\ConvertFrom-HexadecimalDigits.ps1"
}

Describe 'ConvertFrom-HexadecimalDigits' {

    It 'reads <Digits> as <Expected>' -ForEach @(
        @{ Digits = '1F'; Expected = '31' }
        @{ Digits = '1f'; Expected = '31' }
        @{ Digits = '0'; Expected = '0' }
        @{ Digits = '10000000000000000'; Expected = '18446744073709551616' }
    ) {

        # ARRANGE
        $expectedNumber = [System.Numerics.BigInteger]::Parse($Expected)

        # ACT
        $actualNumber = ConvertFrom-HexadecimalDigits -Digits $Digits

        # ASSERT
        $actualNumber |
            Should -Be $expectedNumber
    }

    It 'reads <Digits>, whose first digit is high, as a positive number' -ForEach @(
        @{ Digits = 'F'; Expected = '15' }
        @{ Digits = 'FFFFFFFF'; Expected = '4294967295' }
    ) {

        # ARRANGE
        $expectedNumber = [System.Numerics.BigInteger]::Parse($Expected)

        # ACT
        $actualNumber = ConvertFrom-HexadecimalDigits -Digits $Digits

        # ASSERT
        $actualNumber |
            Should -Be $expectedNumber
    }
}
