BeforeAll {

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$libRoot\ConvertFrom-LogLine.ps1"
    . "$libRoot\ConvertFrom-ThrowableHeader.ps1"
    . "$libRoot\ConvertFrom-XStreamClassLine.ps1"
    . "$libRoot\Get-LoadOutcomeNames.ps1"
    . "$libRoot\Read-LoadAttempt.ps1"

    # The parsers are mocked, so a test line only has to name the loader for the cheap check to pass it on.
    # "<logger>|<level>|<message>" stands for a loader line, "THROW|<type>|<message>" for a throwable header,
    # and "CLASS|<depth>|<class>" for a class line of XStream's debugging information.
    $script:Loader = 'com.fs.starfarer.campaign.save.CampaignGameManager'

    # Not mocked: the reader matches XStream's block opener itself.
    $script:DebugBlockStart = '---- Debugging information ----'

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

        Mock ConvertFrom-LogLine {

            $parts = $Line -split '\|'

            if ($parts.Count -eq 3 -and $parts[0] -eq $LoggerName) {
                [pscustomobject]@{ Level = $parts[1]; Message = $parts[2] }
            }
        }

        Mock ConvertFrom-ThrowableHeader {

            $parts = $Line -split '\|'

            if ($parts[0] -eq 'THROW') {
                [pscustomobject]@{ Type = $parts[1]; Message = $parts[2] }
            }
        }

        Mock ConvertFrom-XStreamClassLine {

            $parts = $Line -split '\|'

            if ($parts[0] -eq 'CLASS') {
                [pscustomobject]@{ Depth = [int] $parts[1]; Class = $parts[2] }
            }
        }
    }

    It 'reads a loaded attempt with its save path, file and line' {

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

    It 'takes the last throwable header of a failure as its root cause, past the ERROR line and stack frames' {

        $log = New-LogFile -Name 'failed.log' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_D..."
            "$script:Loader|INFO|Error loading"
            "$script:Loader|ERROR|Failed calling method"
            'THROW|com.thoughtworks.xstream.converters.ConversionException|Failed calling method'
            "    at $script:Loader.o00000(Unknown Source)"
            'THROW|java.lang.RuntimeException|Weapon spec [gone] not found!'
            "$script:Loader|INFO|Loading stage 11"
            'THROW|java.lang.IllegalStateException|after the cause ended'
        )

        $attempt = (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts[0]

        $attempt.Outcome | Should -Be 'F'
        $attempt.CauseType | Should -Be 'java.lang.RuntimeException'
        $attempt.Cause | Should -Be 'Weapon spec [gone] not found!'
    }

    It 'reads the object chain from the first debugging block of a failure only' {

        $log = New-LogFile -Name 'chain.log' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_G..."
            "$script:Loader|INFO|Error loading"
            "$script:Loader|ERROR|Failed calling method"
            $script:DebugBlockStart
            'CLASS|0|a.Failed'
            'CLASS|1|a.Container'
            'THROW|com.thoughtworks.xstream.converters.ConversionException|Failed calling method'
            $script:DebugBlockStart
            'CLASS|0|a.FromTheSecondBlock'
        )

        $chain = (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts[0].ObjectChain

        $chain.Class | Should -Be @('a.Failed', 'a.Container')
        $chain.Depth | Should -Be @(0, 1)
    }

    It 'gives each failure its own chain, and ignores class lines outside a debugging block' {

        $log = New-LogFile -Name 'chains.log' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_H..."
            "$script:Loader|INFO|Error loading"
            "$script:Loader|ERROR|Failed calling method"
            $script:DebugBlockStart
            'CLASS|0|a.First'
            "$script:Loader|INFO|Loading C:\saves\save_H..."
            "$script:Loader|INFO|Error loading"
            "$script:Loader|ERROR|Failed calling method"
            'CLASS|0|a.OutsideABlock'
            $script:DebugBlockStart
            'CLASS|0|a.Second'
        )

        $attempts = (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts

        $attempts[0].ObjectChain.Class | Should -Be @('a.First')
        $attempts[1].ObjectChain.Class | Should -Be @('a.Second')
    }

    It 'leaves a loaded attempt with no chain' {

        $log = New-LogFile -Name 'nochain.log' -Lines @(
            "$script:Loader|INFO|Loading C:\saves\save_I..."
            "$script:Loader|INFO|Finished loading"
        )

        (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts[0].ObjectChain.Count | Should -Be 0
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
