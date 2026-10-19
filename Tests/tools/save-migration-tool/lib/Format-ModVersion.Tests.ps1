BeforeAll {

    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Get-JsonPropertyValue.ps1"
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Get-VersionObjectParts.ps1"
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Format-ModVersion.ps1"
}

Describe 'Format-ModVersion' {

    BeforeEach {

        Mock Get-JsonPropertyValue { $Object.PSObject.Properties[$Name].Value }
        Mock Get-VersionObjectParts { ,@(2, $null, 3) }
    }

    It 'returns an empty string for no version' {
        Format-ModVersion -Version $null | Should -Be ''
    }

    It 'returns a string version as it is' {
        Format-ModVersion -Version '1.3.1b' | Should -Be '1.3.1b'
    }

    It 'joins the parts of the object form, skipping a missing one' {
        Format-ModVersion -Version ([pscustomobject]@{ major = 2 }) | Should -Be '2.3'
    }

    It 'prints any other value as text' {
        Format-ModVersion -Version 1.5 | Should -Be '1.5'
    }
}
