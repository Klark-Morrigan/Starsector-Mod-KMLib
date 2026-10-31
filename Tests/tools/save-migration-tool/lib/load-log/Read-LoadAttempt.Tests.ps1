BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\New-LoadLogScan.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Read-LoadLogFile.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Read-LoadAttempt.ps1"
}

Describe 'Read-LoadAttempt' {

    BeforeEach {

        Mock New-LoadLogScan {
            [pscustomobject]@{
                Base                = $SaveBaseDirectory
                Attempts            = [System.Collections.Generic.List[object]]::new()
                TraceLinesByAttempt = @{ 'marker' = 'traces' }
            }
        }

        # Each file adds one attempt naming it and the base directory the scan carries.
        Mock Read-LoadLogFile {

            $Scan.Attempts.Add("$LogFile in $($Scan.Base)")

            [pscustomobject]@{ LogFile = $LogFile }
        }
    }

    It 'reads every file, in order, into one scan, and returns its attempts, traces and each file''s read' {

        # ARRANGE
        $expectedAttempts = @('a.log.1 in C:\core', 'a.log in C:\core')
        $expectedTraces = 'traces'
        $expectedFileReads = @('a.log.1', 'a.log')

        # ACT
        $actualRead = Read-LoadAttempt -LogFiles @('a.log.1', 'a.log') -SaveBaseDirectory 'C:\core'

        # ASSERT
        $actualRead.Attempts | Should -Be $expectedAttempts
        $actualRead.TraceLinesByAttempt['marker'] | Should -Be $expectedTraces
        $actualRead.FileReads.LogFile | Should -Be $expectedFileReads

        Should -Invoke New-LoadLogScan -Times 1 -Exactly
    }
}
