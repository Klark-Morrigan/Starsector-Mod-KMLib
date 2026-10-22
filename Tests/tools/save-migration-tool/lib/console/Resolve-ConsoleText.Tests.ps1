BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Test-ConsoleStreamRedirected.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Resolve-ConsoleText.ps1"
}

Describe 'Resolve-ConsoleText' {

    BeforeAll {
        $script:Painted = "read `e[97ma.log`e[0m in `e[38;2;255;190;70m3 ms`e[0m"
    }

    BeforeEach {

        $script:OriginalNoColor = $env:NO_COLOR
        $env:NO_COLOR = $null
    }

    AfterEach {
        $env:NO_COLOR = $script:OriginalNoColor
    }

    It 'keeps the colour on a <StreamName> stream that reaches a console' -ForEach @(
        @{ StreamName = 'Output' }
        @{ StreamName = 'Error' }
    ) {
        Mock Test-ConsoleStreamRedirected { $false }

        Resolve-ConsoleText -Text $script:Painted -Stream $StreamName | Should -Be $script:Painted
        Should -Invoke Test-ConsoleStreamRedirected -ParameterFilter { $Stream -eq $StreamName } -Times 1 -Exactly
    }

    It 'strips every SGR sequence from a redirected stream' {

        Mock Test-ConsoleStreamRedirected { $true }

        Resolve-ConsoleText -Text $script:Painted -Stream 'Error' | Should -Be 'read a.log in 3 ms'
    }

    It 'strips the colour on a console when NO_COLOR is set' {

        Mock Test-ConsoleStreamRedirected { $false }
        $env:NO_COLOR = '1'

        Resolve-ConsoleText -Text $script:Painted -Stream 'Output' | Should -Be 'read a.log in 3 ms'
    }

    It 'leaves text with no colour as it is' {

        Mock Test-ConsoleStreamRedirected { $true }

        Resolve-ConsoleText -Text 'plain [text]' -Stream 'Output' | Should -Be 'plain [text]'
    }
}
