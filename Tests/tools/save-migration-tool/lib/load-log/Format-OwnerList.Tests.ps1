BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Format-OwnerList.ps1"
}

Describe 'Format-OwnerList' {

    BeforeEach {
        Mock Format-ConsoleText { "<$Role>$Text" }
    }

    It 'paints each owner as a mod, in order, and joins them with commas' {
        Format-OwnerList -Owners @('nexerelin', 'starlords') | Should -Be '<Mod>nexerelin, <Mod>starlords'
    }

    It 'writes one owner alone' {
        Format-OwnerList -Owners @('starlords') | Should -Be '<Mod>starlords'
    }
}
