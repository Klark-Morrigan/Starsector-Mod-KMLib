BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-VersionObjectParts.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Test-VersionObjectForm.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Format-ModVersion.ps1"
}

Describe 'Format-ModVersion' {

    BeforeEach {

        # The object form is any dictionary here; a string or number is not.
        Mock Test-VersionObjectForm { $Version -is [System.Collections.IDictionary] }
        Mock Get-VersionObjectParts { ,@($null, 2, $null) }
    }

    It 'returns an empty string for no version' {

        # ARRANGE
        $expectedText = ''

        # ACT
        $actualText = Format-ModVersion -Version $null

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'joins the parts of the object form, skipping a missing one' {

        # ARRANGE
        $version = [ordered]@{ minor = 2 }
        $expectedText = '2'

        # ACT
        $actualText = Format-ModVersion -Version $version

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'returns <Description> as its text' -ForEach @(
        @{ Description = 'a string'; Version = '1.3.1b'; Text = '1.3.1b' }
        @{ Description = 'a whole number'; Version = [long] 2; Text = '2' }
        @{ Description = 'a decimal number'; Version = 1.5; Text = '1.5' }
    ) {

        # ARRANGE
        $expectedText = $Text

        # ACT
        $actualText = Format-ModVersion -Version $Version

        # ASSERT
        $actualText | Should -Be $expectedText
    }
}
