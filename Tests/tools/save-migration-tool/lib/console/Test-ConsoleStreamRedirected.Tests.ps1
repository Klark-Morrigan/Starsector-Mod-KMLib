BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Test-ConsoleStreamRedirected.ps1"
}

# The answer depends on how the suite runs, so each case compares with what .NET reports for the same stream.
Describe 'Test-ConsoleStreamRedirected' {

    It 'reports stdout as .NET sees it' {

        # ARRANGE
        $expectedIsRedirected = [Console]::IsOutputRedirected

        # ACT
        $actualIsRedirected = Test-ConsoleStreamRedirected -Stream 'Output'

        # ASSERT
        $actualIsRedirected | Should -Be $expectedIsRedirected
    }

    It 'reports stderr as .NET sees it' {

        # ARRANGE
        $expectedIsRedirected = [Console]::IsErrorRedirected

        # ACT
        $actualIsRedirected = Test-ConsoleStreamRedirected -Stream 'Error'

        # ASSERT
        $actualIsRedirected | Should -Be $expectedIsRedirected
    }
}
