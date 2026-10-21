BeforeAll {

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$libRoot\Add-LineRange.ps1"
    . "$libRoot\ConvertFrom-FailureTrace.ps1"
    . "$libRoot\ConvertFrom-LogLine.ps1"
    . "$libRoot\Find-MatchingLineIndex.ps1"
    . "$libRoot\Get-LoadOutcomeNames.ps1"
    . "$libRoot\Read-LoadAttempt.ps1"

    # The parsers are mocked, so a test line only has to name the loader for the index search to find it.
    # "<logger>|<level>|<message>" stands for a loader line; any other line naming the loader stands for a stack frame.
    $script:Loader = 'com.fs.starfarer.campaign.save.CampaignGameManager'

    function script:New-LogFile {
        # Writes one log file under TestDrive from the given lines and returns its path.
        param(
            [string] $Name,
            [string[]] $Lines
        )

        $path = Join-Path $TestDrive $Name

        [System.IO.File]::WriteAllLines($path, $Lines)

        return $path
    }
}

Describe 'Read-LoadAttempt' {

    BeforeEach {

        Mock Get-LoadOutcomeNames { [pscustomobject]@{ Loaded = 'L'; Failed = 'F'; Unfinished = 'U' } }

        Mock Find-MatchingLineIndex {

            for ($index = 0; $index -lt $Lines.Count; $index++) {
                if ($Lines[$index].Contains($Text)) { $index }
            }
        }

        Mock Add-LineRange {
            for ($index = $From; $index -le $To; $index++) { $Target.Add($Lines[$index]) }
        }

        Mock ConvertFrom-LogLine {

            $parts = $Line -split '\|'

            if ($parts.Count -eq 3 -and $parts[0] -eq $LoggerName) {
                [pscustomobject]@{ Level = $parts[1]; Message = $parts[2] }
            }
        }

        # The trace's lines, joined, stand for its cause; its first line stands for its object chain.
        Mock ConvertFrom-FailureTrace {

            [pscustomobject]@{
                CauseType   = 'T'
                Cause       = $Lines -join '/'
                ObjectChain = @($Lines | Select-Object -First 1)
            }
        }
    }

    It 'reads a loaded attempt with its save path, file and line, and no cause' {

        $log = New-LogFile -Name 'loaded.log' -Lines @(
            'an unrelated line'
            "$script:Loader|INFO|Loading C:\Games\saves\save_A..."
            "$script:Loader|INFO|Finished loading"
        )

        $read = Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\Games\starsector-core'

        $read.Attempts.Count | Should -Be 1
        $read.Attempts[0].SavePath | Should -Be 'C:\Games\saves\save_A'
        $read.Attempts[0].Outcome | Should -Be 'L'
        $read.Attempts[0].LogFile | Should -Be $log
        $read.Attempts[0].LogLine | Should -Be 2
        $read.Attempts[0].Cause | Should -BeNullOrEmpty
        $read.Attempts[0].ObjectChain.Count | Should -Be 0

        Should -Invoke ConvertFrom-FailureTrace -Times 0 -Exactly
    }

    It 'parses only the lines that name the loader' {

        $log = New-LogFile -Name 'gate.log' -Lines @('an unrelated line', "$script:Loader|INFO|Loading stage 2")

        Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\' | Out-Null

        Should -Invoke ConvertFrom-LogLine -Times 1 -Exactly
    }

    It 'resolves a relative save path against the base directory' {

        $log = New-LogFile -Name 'relative.log' -Lines @("$script:Loader|INFO|Loading ..\saves\save_B...")

        $read = Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\Games\starsector-core'

        $read.Attempts[0].SavePath | Should -Be 'C:\Games\saves\save_B'
    }

    It 'marks an attempt the log ends inside as unfinished' {

        $log = New-LogFile -Name 'unfinished.log' -Lines @("$script:Loader|INFO|Loading C:\saves\save_C...")

        (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts[0].Outcome | Should -Be 'U'
    }

    It 'reads a failure''s trace up to the loader''s next line, past its ERROR lines, stack frames naming it included' {

        $log = New-LogFile -Name 'failed.log' -Lines @(
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

        $attempt = (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts[0]

        $attempt.Outcome | Should -Be 'F'
        $attempt.CauseType | Should -Be 'T'
        $attempt.Cause | Should -Be "first trace line/    at $script:Loader.o00000(Unknown Source)/last trace line"
        $attempt.ObjectChain | Should -Be @('first trace line')
    }

    It 'carries a trace across files, and to the log''s end' {

        $older = New-LogFile -Name 'trace.log.1' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_E..."
            "$script:Loader|INFO|Error loading"
            'in the older file'
        )

        $newer = New-LogFile -Name 'trace.log' -Lines @('in the newer file')
        $attempt = (Read-LoadAttempt -LogFiles @($older, $newer) -SaveBaseDirectory 'C:\').Attempts[0]

        $attempt.Cause | Should -Be 'in the older file/in the newer file'
    }

    It 'gives each failure its own trace' {

        $log = New-LogFile -Name 'traces.log' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_H..."
            "$script:Loader|INFO|Error loading"
            'first'
            "$script:Loader|INFO|Loading C:\saves\save_H..."
            "$script:Loader|INFO|Error loading"
            'second'
        )

        (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts.Cause | Should -Be @('first', 'second')
    }

    It 'carries an attempt across files, and counts the lines and attempts each file holds' {

        $older = New-LogFile -Name 'starsector.log.1' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_F..."
            "$script:Loader|INFO|Loading stage 2"
        )

        $newer = New-LogFile -Name 'starsector.log' -Lines @("$script:Loader|INFO|Finished loading")
        $read = Read-LoadAttempt -LogFiles @($older, $newer) -SaveBaseDirectory 'C:\'

        $read.Attempts.Count | Should -Be 1
        $read.Attempts[0].Outcome | Should -Be 'L'
        $read.FileReads.LineCount | Should -Be @(2, 1)
        $read.FileReads.AttemptCount | Should -Be @(1, 0)
    }

    It 'ignores a finishing line with no attempt open' {

        $log = New-LogFile -Name 'stray.log' -Lines @("$script:Loader|INFO|Finished loading")

        (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts.Count | Should -Be 0
    }
}
