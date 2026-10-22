BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Resolve-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Resolve-OutputLines.ps1"
}

Describe 'Resolve-OutputLines' {

    BeforeEach {
        Mock Resolve-ConsoleText { "[$Stream]$Text" }
    }

    It 'resolves each line, blank ones included, as stdout takes it' {
        Resolve-OutputLines -Lines @('', 'one', 'two') | Should -Be @('[Output]', '[Output]one', '[Output]two')
    }
}
