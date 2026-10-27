BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\ConvertFrom-VersionText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-VersionObjectParts.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\New-VersionInfo.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Test-VersionObjectForm.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\ConvertTo-VersionInfo.ps1"
}

Describe 'ConvertTo-VersionInfo' {

    BeforeEach {

        Mock Test-VersionObjectForm { "$Version" -eq 'object form' }
        Mock Get-VersionObjectParts { ,@('1', '2', $null) }
        Mock New-VersionInfo { "parts $Major|$Minor|$Patch|$MinorIsRC" }
        Mock ConvertFrom-VersionText { "text [$Text]" }
    }

    It 'takes the object form part by part, never as a release candidate' {
        ConvertTo-VersionInfo -Version 'object form' | Should -Be 'parts 1|2||False'
    }

    It 'reads <Case> as text' -ForEach @(
        @{ Case = 'a string'; Version = '1.2.3'; Expected = 'text [1.2.3]' }
        @{ Case = 'a bare number'; Version = 1.5; Expected = 'text [1.5]' }
        @{ Case = 'no version'; Version = $null; Expected = 'text []' }
    ) {
        ConvertTo-VersionInfo -Version $Version | Should -Be $Expected
    }
}
