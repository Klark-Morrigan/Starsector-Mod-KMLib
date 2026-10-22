BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Get-ConsolePalette.ps1"
}

Describe 'Get-ConsolePalette' {

    BeforeAll {
        $script:Palette = Get-ConsolePalette
    }

    It 'gives <Role> the SGR sequence <Sequence>' -ForEach @(
        @{ Role = 'Reset'; Sequence = '[0m' }
        @{ Role = 'Script'; Sequence = '[90m' }
        @{ Role = 'Time'; Sequence = '[90m' }
        @{ Role = 'Error'; Sequence = '[31m' }
        @{ Role = 'Success'; Sequence = '[32m' }
        @{ Role = 'Warning'; Sequence = '[38;2;255;190;70m' }
        @{ Role = 'Mod'; Sequence = '[38;2;255;230;0m' }
        @{ Role = 'Content'; Sequence = '[38;2;249;232;81m' }
        @{ Role = 'Path'; Sequence = '[38;2;242;234;162m' }
        @{ Role = 'Code'; Sequence = '[36m' }
        @{ Role = 'Number'; Sequence = '[38;2;128;215;128m' }
    ) {
        $script:Palette[$Role] | Should -Be "`e$Sequence"
    }

    It 'gives no role beyond those' {
        $script:Palette.Count | Should -Be 11
    }
}
