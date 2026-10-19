BeforeAll {

    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Get-LoadOutcomeNames.ps1"
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Read-LoadAttempt.ps1"

    $script:LoaderPrefix = '[Thread-3] INFO  com.fs.starfarer.campaign.save.CampaignGameManager  - '
    $script:LoaderErrorPrefix = '[Thread-3] ERROR com.fs.starfarer.campaign.save.CampaignGameManager  - '

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
    }

    It 'reads a loaded attempt with its save path, file and line' {

        $log = New-LogFile -Name 'loaded.log' -Lines @(
            '50 [main] INFO  some.Other  - unrelated'
            "100 $script:LoaderPrefix" + 'Loading C:\Games\saves\save_A...'
            "200 $script:LoaderPrefix" + 'Finished loading'
        )

        $read = Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\Games\starsector-core'

        $read.Attempts.Count | Should -Be 1
        $read.Attempts[0].SavePath | Should -Be 'C:\Games\saves\save_A'
        $read.Attempts[0].Outcome | Should -Be 'L'
        $read.Attempts[0].LogFile | Should -Be $log
        $read.Attempts[0].LogLine | Should -Be 2
    }

    It 'resolves a relative save path against the base directory' {

        $log = New-LogFile -Name 'relative.log' -Lines @("100 $script:LoaderPrefix" + 'Loading ..\saves\save_B...')

        $read = Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\Games\starsector-core'

        $read.Attempts[0].SavePath | Should -Be 'C:\Games\saves\save_B'
    }

    It 'marks an attempt the log ends inside as unfinished' {

        $log = New-LogFile -Name 'unfinished.log' -Lines @("100 $script:LoaderPrefix" + 'Loading C:\saves\save_C...')

        (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts[0].Outcome | Should -Be 'U'
    }

    It 'reads the last throwable header of a failure as its root cause, past a loader stack frame' {

        $log = New-LogFile -Name 'failed.log' -Lines @(
            "100 $script:LoaderPrefix" + 'Loading C:\saves\save_D...'
            "200 $script:LoaderPrefix" + 'Error loading'
            "200 $script:LoaderErrorPrefix" + 'Failed calling method'
            'com.thoughtworks.xstream.converters.ConversionException: Failed calling method'
            '	at com.fs.starfarer.campaign.save.CampaignGameManager.o00000(Unknown Source)'
            'Caused by: java.lang.RuntimeException: Weapon spec [gone] not found!'
            '	at com.fs.starfarer.loading.Q.String(Unknown Source)'
            "300 $script:LoaderPrefix" + 'Loading stage 11'
        )

        $attempt = (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts[0]

        $attempt.Outcome | Should -Be 'F'
        $attempt.CauseType | Should -Be 'java.lang.RuntimeException'
        $attempt.Cause | Should -Be 'Weapon spec [gone] not found!'
    }

    It 'reads a throwable with no message as an empty cause' {

        $log = New-LogFile -Name 'nomessage.log' -Lines @(
            "100 $script:LoaderPrefix" + 'Loading C:\saves\save_E...'
            "200 $script:LoaderPrefix" + 'Error loading'
            'java.lang.NullPointerException'
        )

        $attempt = (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts[0]

        $attempt.CauseType | Should -Be 'java.lang.NullPointerException'
        $attempt.Cause | Should -Be ''
    }

    It 'carries an attempt across files, and counts the lines and attempts each file holds' {

        $older = New-LogFile -Name 'starsector.log.1' -Lines @(
            "100 $script:LoaderPrefix" + 'Loading C:\saves\save_F...'
            "110 $script:LoaderPrefix" + 'Loading stage 2'
        )
        $newer = New-LogFile -Name 'starsector.log' -Lines @(
            "200 $script:LoaderPrefix" + 'Finished loading'
        )

        $read = Read-LoadAttempt -LogFiles @($older, $newer) -SaveBaseDirectory 'C:\'

        $read.Attempts.Count | Should -Be 1
        $read.Attempts[0].Outcome | Should -Be 'L'
        $read.FileReads.LineCount | Should -Be @(2, 1)
        $read.FileReads.AttemptCount | Should -Be @(1, 0)
    }

    It 'ignores a finishing line with no attempt open' {

        $log = New-LogFile -Name 'stray.log' -Lines @("100 $script:LoaderPrefix" + 'Finished loading')

        (Read-LoadAttempt -LogFiles @($log) -SaveBaseDirectory 'C:\').Attempts.Count | Should -Be 0
    }
}
