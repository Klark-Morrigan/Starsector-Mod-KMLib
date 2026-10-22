BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Get-ConsolePalette.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
}

Describe 'Format-ConsoleText' {

    BeforeEach {
        Mock Get-ConsolePalette { @{ Reset = '</>'; Mod = '<mod>' } }
    }

    It 'wraps the text in its role''s colour and a reset' {
        Format-ConsoleText -Role 'Mod' -Text 'alpha' | Should -Be '<mod>alpha</>'
    }

    It 'fails naming a role the palette lacks' {
        { Format-ConsoleText -Role 'Nope' -Text 'x' } | Should -Throw 'no console colour for role Nope'
    }
}
