BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Test-ConsoleStreamRedirected.ps1"
}

# The answer depends on how the suite runs, so each case compares with what .NET reports for the same stream.
Describe 'Test-ConsoleStreamRedirected' {

    It 'reports stdout as .NET sees it' {
        Test-ConsoleStreamRedirected -Stream 'Output' | Should -Be ([Console]::IsOutputRedirected)
    }

    It 'reports stderr as .NET sees it' {
        Test-ConsoleStreamRedirected -Stream 'Error' | Should -Be ([Console]::IsErrorRedirected)
    }
}
