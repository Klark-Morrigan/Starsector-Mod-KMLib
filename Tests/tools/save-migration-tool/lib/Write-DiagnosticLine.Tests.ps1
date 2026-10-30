BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\console\Resolve-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Get-DiagnosticSourceSlotName.ps1"
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Write-DiagnosticLine.ps1"

    # A slot of the suite's own, so a case never sees what the scripts' runs left in the real one.
    $script:SlotName = 'KMLib.SaveMigrationTool.Tests.LastDiagnosticSource'

    function script:Invoke-WithStderrCaptured {
        # Runs the block with stderr swapped for a buffer; returns what it wrote there, split into lines.
        param([scriptblock] $Block)

        $stderrBuffer = [System.IO.StringWriter]::new()
        $originalStderr = [Console]::Error

        [Console]::SetError($stderrBuffer)

        try {
            $output = & $Block

        } finally {
            [Console]::SetError($originalStderr)
        }

        return [pscustomobject]@{
            Lines  = @($stderrBuffer.ToString().TrimEnd("`r", "`n") -split "`r?`n")
            Output = $output
        }
    }
}

Describe 'Write-DiagnosticLine' {

    BeforeEach {

        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Resolve-ConsoleText { "[$Stream]$Text" }
        Mock Get-DiagnosticSourceSlotName { $script:SlotName }

        [AppDomain]::CurrentDomain.SetData($script:SlotName, $null)
    }

    It 'writes the message to stderr as that stream takes it, opening with the source painted as a script' {

        # ARRANGE
        $expectedLines = @('[Error]<Script>Some-Script: read 3 files')

        # ACT
        $actualRun = Invoke-WithStderrCaptured { Write-DiagnosticLine -Source 'Some-Script' -Message 'read 3 files' }

        # ASSERT
        $actualRun.Lines | Should -Be $expectedLines
        $actualRun.Output | Should -BeNullOrEmpty
    }

    It 'keeps the later lines of a message under its first' {

        # ARRANGE
        $expectedLines = @('[Error]<Script>S: counts,', '     nearest')

        # ACT
        $actualRun = Invoke-WithStderrCaptured { Write-DiagnosticLine -Source 'S' -Message "counts,`n  nearest" }

        # ASSERT
        $actualRun.Lines | Should -Be $expectedLines
    }

    It 'parts a block with a blank line where the source changes, or a block opens, but not before the first line' {

        # ARRANGE
        $expectedLines = @(
            '[Error]<Script>A: one'
            '[Error]<Script>A: two'
            ''
            '[Error]<Script>B: three'
            ''
            '[Error]<Script>B: four'
        )

        # ACT
        $actualRun = Invoke-WithStderrCaptured {

            Write-DiagnosticLine -Source 'A' -Message 'one'
            Write-DiagnosticLine -Source 'A' -Message 'two'
            Write-DiagnosticLine -Source 'B' -Message 'three'
            Write-DiagnosticLine -Source 'B' -Message 'four' -IsBlockStart
        }

        # ASSERT
        $actualRun.Lines | Should -Be $expectedLines
    }
}
