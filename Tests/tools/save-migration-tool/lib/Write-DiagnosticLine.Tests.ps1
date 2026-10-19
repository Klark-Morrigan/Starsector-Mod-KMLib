BeforeAll {
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Write-DiagnosticLine.ps1"
}

Describe 'Write-DiagnosticLine' {

    It 'writes the message to stderr, opening with the source' {

        $stderrBuffer = [System.IO.StringWriter]::new()
        $originalStderr = [Console]::Error

        [Console]::SetError($stderrBuffer)

        try {
            $output = Write-DiagnosticLine -Source 'Some-Script' -Message 'read 3 files'

        } finally {
            [Console]::SetError($originalStderr)
        }

        $stderrBuffer.ToString() | Should -Be "Some-Script: read 3 files$([Environment]::NewLine)"
        $output | Should -BeNullOrEmpty
    }
}
