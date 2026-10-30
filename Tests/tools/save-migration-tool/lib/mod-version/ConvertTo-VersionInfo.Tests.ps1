BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\ConvertFrom-VersionText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\ConvertTo-VersionInfo.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\Get-VersionObjectParts.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\New-VersionInfo.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\Test-VersionObjectForm.ps1"
}

Describe 'ConvertTo-VersionInfo' {

    BeforeEach {

        Mock Test-VersionObjectForm { "$Version" -eq 'object form' }
        Mock Get-VersionObjectParts { ,@('1', '2', $null) }
        Mock New-VersionInfo { "parts $Major|$Minor|$Patch|$MinorIsRC" }
        Mock ConvertFrom-VersionText { "text [$Text]" }
    }

    It 'takes the object form part by part, never as a release candidate' {

        # ARRANGE
        $expectedVersionInfo = 'parts 1|2||False'

        # ACT
        $actualVersionInfo = ConvertTo-VersionInfo -Version 'object form'

        # ASSERT
        $actualVersionInfo | Should -Be $expectedVersionInfo
    }

    It 'reads <Case> as text' -ForEach @(
        @{ Case = 'a string'; Version = '1.2.3'; Expected = 'text [1.2.3]' }
        @{ Case = 'a bare number'; Version = 1.5; Expected = 'text [1.5]' }
        @{ Case = 'no version'; Version = $null; Expected = 'text []' }
    ) {

        # ARRANGE
        $expectedVersionInfo = $Expected

        # ACT
        $actualVersionInfo = ConvertTo-VersionInfo -Version $Version

        # ASSERT
        $actualVersionInfo | Should -Be $expectedVersionInfo
    }
}
