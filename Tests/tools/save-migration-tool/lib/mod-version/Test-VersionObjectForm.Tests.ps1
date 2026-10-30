BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\Test-VersionObjectForm.ps1"
}

Describe 'Test-VersionObjectForm' {

    It 'reads <Description> as the object form, whichever parts it holds' -ForEach @(
        @{ Description = 'a JSON object parsed into a hashtable'
            Version = ('{ "minor": 2 }' | ConvertFrom-Json -AsHashtable) }
        @{ Description = 'an ordered dictionary of parts'; Version = [ordered]@{ major = '1' } }
    ) {

        # ARRANGE
        $expectedIsObjectForm = $true

        # ACT
        $actualIsObjectForm = Test-VersionObjectForm -Version $Version

        # ASSERT
        $actualIsObjectForm | Should -Be $expectedIsObjectForm
    }

    It 'reads <Description> as no object form' -ForEach @(
        @{ Description = 'a string'; Version = '1.2.3' }
        @{ Description = 'a whole number'; Version = [long] 2 }
        @{ Description = 'a decimal number'; Version = 1.5 }
        @{ Description = 'no version'; Version = $null }
    ) {

        # ARRANGE
        $expectedIsObjectForm = $false

        # ACT
        $actualIsObjectForm = Test-VersionObjectForm -Version $Version

        # ASSERT
        $actualIsObjectForm | Should -Be $expectedIsObjectForm
    }
}
