BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Test-VersionObjectForm.ps1"
}

Describe 'Test-VersionObjectForm' {

    It 'reads a parsed JSON object as the object form, whichever parts it holds' {
        Test-VersionObjectForm -Version ('{ "minor": 2 }' | ConvertFrom-Json) | Should -BeTrue
    }

    It 'reads <Description> as no object form' -ForEach @(
        @{ Description = 'a string'; Version = '1.2.3' }
        @{ Description = 'a whole number'; Version = [long] 2 }
        @{ Description = 'a decimal number'; Version = 1.5 }
        @{ Description = 'no version'; Version = $null }
    ) {
        Test-VersionObjectForm -Version $Version | Should -BeFalse
    }
}
