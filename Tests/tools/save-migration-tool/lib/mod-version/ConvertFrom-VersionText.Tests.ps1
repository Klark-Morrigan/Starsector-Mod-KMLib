BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\ConvertFrom-VersionText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\New-VersionInfo.ps1"
}

Describe 'ConvertFrom-VersionText' {

    BeforeEach {
        Mock New-VersionInfo { "$Major|$Minor|$Patch|$MinorIsRC" }
    }

    # Each expectation is what the game's VersionInfo.setFromString keeps for the same text.
    It 'reads <Text> as <Expected>' -ForEach @(
        @{ Text = '1.2.3'; Expected = '1|2|3|False' }
        @{ Text = '1.2'; Expected = '1|2||False' }
        @{ Text = '0.1.98.7'; Expected = '0.1|98|7|False' }
        @{ Text = '1.2.3.4'; Expected = '1.2|3|4|False' }
        @{ Text = '0.9.1'; Expected = '0.9|1||False' }
        @{ Text = '0.9'; Expected = '0.9|||False' }
        @{ Text = '1.14.1aa'; Expected = '1|14|1|False' }
        @{ Text = '1.0.2a'; Expected = '1|0|2|False' }
        @{ Text = '1.5.8rc2'; Expected = '1|5|8rc2|False' }
        @{ Text = '0.98a-RC8'; Expected = '0.98|8||True' }
        @{ Text = '1.2.'; Expected = '1|2||False' }
    ) {

        # ARRANGE
        $expectedParts = $Expected

        # ACT
        $actualParts = ConvertFrom-VersionText -Text $Text

        # ASSERT
        $actualParts | Should -Be $expectedParts
    }

    It 'sets no parts for text of <Case>' -ForEach @(
        @{ Case = 'one part'; Text = '2025-03-29' }
        @{ Case = 'five parts'; Text = '0.0.1-beta.4+bf.1' }
        @{ Case = 'no characters'; Text = '' }
    ) {

        # ARRANGE
        $expectedParts = '|||False'

        # ACT
        $actualParts = ConvertFrom-VersionText -Text $Text

        # ASSERT
        $actualParts | Should -Be $expectedParts
    }
}
