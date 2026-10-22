BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\Add-LineRange.ps1"
    . "$libRoot\ConvertFrom-LogLine.ps1"
    . "$libRoot\Find-MatchingLineIndex.ps1"
    . "$libRoot\Get-LoadOutcomeNames.ps1"
    . "$libRoot\New-LoadLogScan.ps1"
    . "$libRoot\Read-LoadLogFile.ps1"

    # The parsers are mocked, so a test line only has to name the loader for the index search to find it.
    # "<logger>|<level>|<message>" stands for a loader line; any other line naming the loader stands for a stack frame.
    $script:Loader = 'com.fs.starfarer.campaign.save.CampaignGameManager'

    function script:Read-TestLogs {
        # Reads the given log files, oldest first, into one fresh scan, and returns the scan and each file's read.
        param(
            [string[]] $LogFiles,
            [string] $SaveBaseDirectory = 'C:\'
        )

        $scan = New-LoadLogScan -SaveBaseDirectory $SaveBaseDirectory
        $fileReads = @(foreach ($logFile in $LogFiles) { Read-LoadLogFile -LogFile $logFile -Scan $scan })

        return [pscustomobject]@{ Scan = $scan; FileReads = $fileReads }
    }
}

Describe 'Read-LoadLogFile' {

    BeforeEach {

        Mock Get-LoadOutcomeNames { [pscustomobject]@{ Loaded = 'L'; Failed = 'F'; Unfinished = 'U' } }

        Mock Find-MatchingLineIndex {

            for ($index = 0; $index -lt $Lines.Count; $index++) {

                if ($Lines[$index].Contains($Text)) {
                    $index
                }
            }
        }

        Mock Add-LineRange {

            for ($index = $From; $index -le $To; $index++) {
                $Target.Add($Lines[$index])
            }
        }

        Mock ConvertFrom-LogLine {

            $parts = $Line -split '\|'

            if ($parts.Count -eq 3 -and $parts[0] -eq $LoggerName) {
                [pscustomobject]@{ Level = $parts[1]; Message = $parts[2] }
            }
        }
    }

    It 'reads a loaded attempt with its save path, file and line, empty readings and no trace' {

        $log = New-TestFile -Name 'loaded.log' -Lines @(
            'an unrelated line'
            "$script:Loader|INFO|Loading C:\Games\saves\save_A..."
            "$script:Loader|INFO|Finished loading"
        )

        $scan = (Read-TestLogs -LogFiles @($log) -SaveBaseDirectory 'C:\Games\starsector-core').Scan
        $attempt = $scan.Attempts[0]

        $scan.Attempts.Count | Should -Be 1
        $attempt.SavePath | Should -Be 'C:\Games\saves\save_A'
        $attempt.Outcome | Should -Be 'L'
        $attempt.LogFile | Should -Be $log
        $attempt.LogLine | Should -Be 2
        $attempt.Cause | Should -BeNullOrEmpty
        $attempt.Missing | Should -BeNullOrEmpty
        $attempt.ObjectChain.Count | Should -Be 0
        $scan.TraceLinesByAttempt.Count | Should -Be 0
    }

    It 'parses only the lines that name the loader' {

        $log = New-TestFile -Name 'gate.log' -Lines @('an unrelated line', "$script:Loader|INFO|Loading stage 2")

        Read-TestLogs -LogFiles @($log) | Out-Null

        Should -Invoke ConvertFrom-LogLine -Times 1 -Exactly
    }

    It 'resolves a relative save path against the base directory' {

        $log = New-TestFile -Name 'relative.log' -Lines @("$script:Loader|INFO|Loading ..\saves\save_B...")

        $scan = (Read-TestLogs -LogFiles @($log) -SaveBaseDirectory 'C:\Games\starsector-core').Scan

        $scan.Attempts[0].SavePath | Should -Be 'C:\Games\saves\save_B'
    }

    It 'marks an attempt the log ends inside as unfinished' {

        $log = New-TestFile -Name 'unfinished.log' -Lines @("$script:Loader|INFO|Loading C:\saves\save_C...")

        (Read-TestLogs -LogFiles @($log)).Scan.Attempts[0].Outcome | Should -Be 'U'
    }

    It 'files a failure''s trace up to the loader''s next line, past its ERROR lines, stack frames naming it included' {

        $log = New-TestFile -Name 'failed.log' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_D..."
            "$script:Loader|INFO|Error loading"
            "$script:Loader|ERROR|Failed calling method"
            'first trace line'
            "    at $script:Loader.o00000(Unknown Source)"
            "$script:Loader|ERROR|a second error line"
            'last trace line'
            "$script:Loader|INFO|Loading stage 11"
            'after the trace ended'
        )

        $scan = (Read-TestLogs -LogFiles @($log)).Scan
        $attempt = $scan.Attempts[0]

        $attempt.Outcome | Should -Be 'F'
        $scan.TraceLinesByAttempt[$attempt] | Should -Be @(
            'first trace line'
            "    at $script:Loader.o00000(Unknown Source)"
            'last trace line'
        )
    }

    It 'carries a trace across files, and to the log''s end' {

        $older = New-TestFile -Name 'trace.log.1' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_E..."
            "$script:Loader|INFO|Error loading"
            'in the older file'
        )

        $newer = New-TestFile -Name 'trace.log' -Lines @('in the newer file')
        $scan = (Read-TestLogs -LogFiles @($older, $newer)).Scan

        $scan.TraceLinesByAttempt[$scan.Attempts[0]] | Should -Be @('in the older file', 'in the newer file')
    }

    It 'files each failure''s trace under its own attempt' {

        $log = New-TestFile -Name 'traces.log' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_H..."
            "$script:Loader|INFO|Error loading"
            'first'
            "$script:Loader|INFO|Loading C:\saves\save_H..."
            "$script:Loader|INFO|Error loading"
            'second'
        )

        $scan = (Read-TestLogs -LogFiles @($log)).Scan

        $scan.TraceLinesByAttempt[$scan.Attempts[0]] | Should -Be @('first')
        $scan.TraceLinesByAttempt[$scan.Attempts[1]] | Should -Be @('second')
    }

    It 'carries an attempt across files, and counts the lines and attempts each file holds' {

        $older = New-TestFile -Name 'starsector.log.1' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_F..."
            "$script:Loader|INFO|Loading stage 2"
        )

        $newer = New-TestFile -Name 'starsector.log' -Lines @("$script:Loader|INFO|Finished loading")
        $read = Read-TestLogs -LogFiles @($older, $newer)

        $read.Scan.Attempts.Count | Should -Be 1
        $read.Scan.Attempts[0].Outcome | Should -Be 'L'
        $read.FileReads.LogFile | Should -Be @($older, $newer)
        $read.FileReads.LineCount | Should -Be @(2, 1)
        $read.FileReads.AttemptCount | Should -Be @(1, 0)
    }

    It 'ignores a finishing line with no attempt open' {

        $log = New-TestFile -Name 'stray.log' -Lines @("$script:Loader|INFO|Finished loading")

        (Read-TestLogs -LogFiles @($log)).Scan.Attempts.Count | Should -Be 0
    }
}
