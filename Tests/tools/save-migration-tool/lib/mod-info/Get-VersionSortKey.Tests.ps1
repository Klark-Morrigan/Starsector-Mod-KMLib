BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-VersionObjectParts.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Test-VersionObjectForm.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-VersionSortKey.ps1"

    function script:Format-ExpectedKey {
        # The key for four numbers, each padded to the key's width, so a case states its numbers alone.
        param([string[]] $Numbers)

        return @($Numbers | ForEach-Object { $_.PadLeft(20, '0') }) -join '.'
    }
}

Describe 'Get-VersionSortKey' {

    BeforeEach {

        Mock Test-VersionObjectForm { $Version -is [System.Management.Automation.PSCustomObject] }
        Mock Get-VersionObjectParts { ,@(2, $null, 3) }
    }

    It 'pads <Version> to <Numbers>' -ForEach @(
        @{ Version = '1.10.0'; Numbers = @('1', '10', '0', '0') }
        @{ Version = '91.DEV'; Numbers = @('91', '0', '0', '0') }
        @{ Version = '1.3.1b'; Numbers = @('1', '3', '1', '0') }
        @{ Version = '0.1.98-RC2'; Numbers = @('0', '1', '98', '0') }
        @{ Version = '1.0.20240615123045'; Numbers = @('1', '0', '20240615123045', '0') }
        @{ Version = '007.1'; Numbers = @('7', '1', '0', '0') }
    ) {
        Get-VersionSortKey -Version $Version | Should -Be (Format-ExpectedKey $Numbers)
    }

    It 'sorts <Higher> above <Lower>' -ForEach @(
        @{ Higher = '1.10.0'; Lower = '1.9.0' }
        @{ Higher = '1.0.20240615123045'; Lower = '1.0.999999' }
        @{ Higher = '2'; Lower = '1.5' }
    ) {
        (Get-VersionSortKey -Version $Higher) -gt (Get-VersionSortKey -Version $Lower) | Should -BeTrue
    }

    It 'reads a <Description> as its digits' -ForEach @(
        @{ Description = 'decimal number'; Version = 1.5; Numbers = @('1', '5', '0', '0') }
        @{ Description = 'whole number'; Version = [long] 2; Numbers = @('2', '0', '0', '0') }
    ) {
        Get-VersionSortKey -Version $Version | Should -Be (Format-ExpectedKey $Numbers)
    }

    It 'reads the object form through its parts, a missing part as zero' {

        Get-VersionSortKey -Version ([pscustomobject]@{ major = 2 }) |
            Should -Be (Format-ExpectedKey '2', '0', '3', '0')

        Should -Invoke Get-VersionObjectParts -Times 1 -Exactly
    }

    It 'reads no version as all zeros' {
        Get-VersionSortKey -Version $null | Should -Be (Format-ExpectedKey '0', '0', '0', '0')
    }
}
