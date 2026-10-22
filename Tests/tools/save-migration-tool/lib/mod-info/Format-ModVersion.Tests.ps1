BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-VersionObjectParts.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Test-VersionObjectForm.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Format-ModVersion.ps1"
}

Describe 'Format-ModVersion' {

    BeforeEach {

        # The object form is any pscustomobject here; a string or number is not.
        Mock Test-VersionObjectForm { $Version -is [System.Management.Automation.PSCustomObject] }
        Mock Get-VersionObjectParts { ,@($null, 2, $null) }
    }

    It 'returns an empty string for no version' {
        Format-ModVersion -Version $null | Should -Be ''
    }

    It 'joins the parts of the object form, skipping a missing one' {
        Format-ModVersion -Version ([pscustomobject]@{ minor = 2 }) | Should -Be '2'
    }

    It 'returns <Description> as its text' -ForEach @(
        @{ Description = 'a string'; Version = '1.3.1b'; Text = '1.3.1b' }
        @{ Description = 'a whole number'; Version = [long] 2; Text = '2' }
        @{ Description = 'a decimal number'; Version = 1.5; Text = '1.5' }
    ) {
        Format-ModVersion -Version $Version | Should -Be $Text
    }
}
