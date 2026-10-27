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
        Format-ModDriftVersion -Version '1.2.3' | Should -Be '<Mod>1.2.3'
    }

    It 'says when a side gives no version' {
        Format-ModDriftVersion -Version '' | Should -Be 'no version'
    }
}
