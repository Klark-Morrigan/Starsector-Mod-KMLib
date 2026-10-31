BeforeAll {

    Set-StrictMode -Version Latest

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

        # ARRANGE
        Mock Test-ConsoleStreamRedirected { $false }

        $expectedText = "read `e[97ma.log`e[0m in `e[38;2;255;190;70m3 ms`e[0m"

        # ACT
        $actualText = Resolve-ConsoleText -Text $script:Painted -Stream $StreamName

        # ASSERT
        $actualText | Should -Be $expectedText
        Should -Invoke Test-ConsoleStreamRedirected -ParameterFilter { $Stream -eq $StreamName } -Times 1 -Exactly
    }

    It 'strips every SGR sequence from a redirected stream' {

        # ARRANGE
        Mock Test-ConsoleStreamRedirected { $true }

        $expectedText = 'read a.log in 3 ms'

        # ACT
        $actualText = Resolve-ConsoleText -Text $script:Painted -Stream 'Error'

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'strips the colour on a console when NO_COLOR is set' {

        # ARRANGE
        Mock Test-ConsoleStreamRedirected { $false }
        $env:NO_COLOR = '1'

        $expectedText = 'read a.log in 3 ms'

        # ACT
        $actualText = Resolve-ConsoleText -Text $script:Painted -Stream 'Output'

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'leaves text with no colour as it is' {

        # ARRANGE
        Mock Test-ConsoleStreamRedirected { $true }

        $expectedText = 'plain [text]'

        # ACT
        $actualText = Resolve-ConsoleText -Text 'plain [text]' -Stream 'Output'

        # ASSERT
        $actualText | Should -Be $expectedText
    }
}
