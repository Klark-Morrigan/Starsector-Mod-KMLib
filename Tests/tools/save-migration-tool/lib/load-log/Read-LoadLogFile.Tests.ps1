BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\Add-LineRange.ps1"
    . "$libRoot\ConvertFrom-LogLine.ps1"
    . "$libRoot\Find-MatchingLineIndex.ps1"
    . "$libRoot\Get-LoadOutcomeNames.ps1"
    . "$libRoot\New-FailureReading.ps1"
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
        Mock New-FailureReading { [ordered]@{ Reading = 'empty' } }

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

    It 'reads a loaded attempt with its save path, file and line, and no trace' {

        # ARRANGE
        $log = New-TestFile -Name 'loaded.log' -Lines @(
            'an unrelated line'
            "$script:Loader|INFO|Loading C:\Games\saves\save_A..."
            "$script:Loader|INFO|Finished loading"
        )

        $expectedAttemptCount = 1
        $expectedSavePath = 'C:\Games\saves\save_A'
        $expectedOutcome = 'L'
        $expectedLogLine = 2

        # ACT
        $actualScan = (Read-TestLogs -LogFiles @($log) -SaveBaseDirectory 'C:\Games\starsector-core').Scan

        # ASSERT
        $actualScan.Attempts.Count | Should -Be $expectedAttemptCount
        $actualScan.Attempts[0].SavePath | Should -Be $expectedSavePath
        $actualScan.Attempts[0].Outcome | Should -Be $expectedOutcome
        $actualScan.Attempts[0].LogFile | Should -Be $log
        $actualScan.Attempts[0].LogLine | Should -Be $expectedLogLine
        $actualScan.TraceLinesByAttempt.Count | Should -Be 0
    }

    # The field order is the JSON's; Add-FailureReading fills the readings once the trace is read.
    It 'gives the attempt empty readings between its outcome and its log position' {

        # ARRANGE
        $log = New-TestFile -Name 'order.log' -Lines @("$script:Loader|INFO|Loading C:\saves\save_I...")

        $expectedNames = @('SavePath', 'Outcome', 'Reading', 'LogFile', 'LogLine')
        $expectedReading = 'empty'

        # ACT
        $actualAttempt = (Read-TestLogs -LogFiles @($log)).Scan.Attempts[0]

        # ASSERT
        $actualAttempt.PSObject.Properties.Name | Should -Be $expectedNames
        $actualAttempt.Reading | Should -Be $expectedReading
    }

    It 'parses only the lines that name the loader' {

        # ARRANGE
        $log = New-TestFile -Name 'gate.log' -Lines @('an unrelated line', "$script:Loader|INFO|Loading stage 2")

        # ACT
        Read-TestLogs -LogFiles @($log) | Out-Null

        # ASSERT
        Should -Invoke ConvertFrom-LogLine -Times 1 -Exactly
    }

    # The game logs the save path as it was handed, relative to its working directory.
    It 'resolves a relative save path against the base directory' {

        # ARRANGE
        $log = New-TestFile -Name 'relative.log' -Lines @("$script:Loader|INFO|Loading ..\saves\save_B...")

        $expectedSavePath = 'C:\Games\saves\save_B'

        # ACT
        $actualScan = (Read-TestLogs -LogFiles @($log) -SaveBaseDirectory 'C:\Games\starsector-core').Scan

        # ASSERT
        $actualScan.Attempts[0].SavePath | Should -Be $expectedSavePath
    }

    # A log ending inside an attempt means the game crashed or is still loading.
    It 'marks an attempt the log ends inside as unfinished' {

        # ARRANGE
        $log = New-TestFile -Name 'unfinished.log' -Lines @("$script:Loader|INFO|Loading C:\saves\save_C...")

        $expectedOutcome = 'U'

        # ACT
        $actualScan = (Read-TestLogs -LogFiles @($log)).Scan

        # ASSERT
        $actualScan.Attempts[0].Outcome | Should -Be $expectedOutcome
    }

    It 'files a failure''s trace up to the loader''s next line, past its ERROR lines, stack frames naming it included' {

        # ARRANGE
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

        $expectedOutcome = 'F'
        $expectedTraceLines = @(
            'first trace line'
            "    at $script:Loader.o00000(Unknown Source)"
            'last trace line'
        )

        # ACT
        $actualScan = (Read-TestLogs -LogFiles @($log)).Scan

        # ASSERT
        $actualScan.Attempts[0].Outcome | Should -Be $expectedOutcome
        $actualScan.TraceLinesByAttempt[$actualScan.Attempts[0]] | Should -Be $expectedTraceLines
    }

    # The log can rotate between two lines of one trace.
    It 'carries a trace across files, and to the log''s end' {

        # ARRANGE
        $older = New-TestFile -Name 'trace.log.1' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_E..."
            "$script:Loader|INFO|Error loading"
            'in the older file'
        )

        $newer = New-TestFile -Name 'trace.log' -Lines @('in the newer file')
        $expectedTraceLines = @('in the older file', 'in the newer file')

        # ACT
        $actualScan = (Read-TestLogs -LogFiles @($older, $newer)).Scan

        # ASSERT
        $actualScan.TraceLinesByAttempt[$actualScan.Attempts[0]] | Should -Be $expectedTraceLines
    }

    It 'files each failure''s trace under its own attempt' {

        # ARRANGE
        $log = New-TestFile -Name 'traces.log' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_H..."
            "$script:Loader|INFO|Error loading"
            'first'
            "$script:Loader|INFO|Loading C:\saves\save_H..."
            "$script:Loader|INFO|Error loading"
            'second'
        )

        $expectedFirstTrace = @('first')
        $expectedSecondTrace = @('second')

        # ACT
        $actualScan = (Read-TestLogs -LogFiles @($log)).Scan

        # ASSERT
        $actualScan.TraceLinesByAttempt[$actualScan.Attempts[0]] | Should -Be $expectedFirstTrace
        $actualScan.TraceLinesByAttempt[$actualScan.Attempts[1]] | Should -Be $expectedSecondTrace
    }

    It 'carries an attempt across files, and counts the lines and attempts each file holds' {

        # ARRANGE
        $older = New-TestFile -Name 'starsector.log.1' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_F..."
            "$script:Loader|INFO|Loading stage 2"
        )

        $newer = New-TestFile -Name 'starsector.log' -Lines @("$script:Loader|INFO|Finished loading")

        $expectedAttemptCount = 1
        $expectedOutcome = 'L'
        $expectedLogFiles = @($older, $newer)
        $expectedLineCounts = @(2, 1)
        $expectedAttemptCounts = @(1, 0)

        # ACT
        $actualRead = Read-TestLogs -LogFiles @($older, $newer)

        # ASSERT
        $actualRead.Scan.Attempts.Count | Should -Be $expectedAttemptCount
        $actualRead.Scan.Attempts[0].Outcome | Should -Be $expectedOutcome
        $actualRead.FileReads.LogFile | Should -Be $expectedLogFiles
        $actualRead.FileReads.LineCount | Should -Be $expectedLineCounts
        $actualRead.FileReads.AttemptCount | Should -Be $expectedAttemptCounts
    }

    # The oldest log file may open after an attempt began in a file already rotated away.
    It 'ignores a finishing line with no attempt open' {

        # ARRANGE
        $log = New-TestFile -Name 'stray.log' -Lines @("$script:Loader|INFO|Finished loading")

        # ACT
        $actualScan = (Read-TestLogs -LogFiles @($log)).Scan

        # ASSERT
        $actualScan.Attempts.Count | Should -Be 0
    }
}
