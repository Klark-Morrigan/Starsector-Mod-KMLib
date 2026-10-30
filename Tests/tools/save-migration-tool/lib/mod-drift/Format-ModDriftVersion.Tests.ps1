BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Format-ModDriftVersion.ps1"
}

Describe 'Format-ModDriftVersion' {

    BeforeEach {
        Mock Format-ConsoleText { "<$Role>$Text" }
    }

    It 'paints a version as a mod value' {

        # ARRANGE
        $expectedText = '<Mod>1.2.3'

        # ACT
        $actualText = Format-ModDriftVersion -Version '1.2.3'

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'says when a side gives no version' {

        # ARRANGE
        $expectedText = 'no version'

        # ACT
        $actualText = Format-ModDriftVersion -Version ''

        # ASSERT
        $actualText | Should -Be $expectedText
    }
}
