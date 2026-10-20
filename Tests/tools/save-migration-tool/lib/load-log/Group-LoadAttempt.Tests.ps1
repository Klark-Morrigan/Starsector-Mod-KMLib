BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Get-LoadOutcomeNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Group-LoadAttempt.ps1"

    function script:New-Attempt {
        # One attempt as Read-LoadAttempt returns it.
        param(
            [string] $SavePath,
            [string] $Outcome,
            [string] $Cause = $null,
            [int] $LogLine = 1
        )

        return [pscustomobject]@{
            SavePath  = $SavePath
            Outcome   = $Outcome
            CauseType = if ($Cause) { 'java.lang.RuntimeException' } else { $null }
            Cause     = $Cause
            LogFile   = 'C:\starsector.log'
            LogLine   = $LogLine
        }
    }
}

Describe 'Group-LoadAttempt' {

    BeforeEach {
        Mock Get-LoadOutcomeNames { [pscustomobject]@{ Loaded = 'L'; Failed = 'F'; Unfinished = 'U' } }
    }

    It 'folds failed retries of one save into one load, naming each source' {

        $loads = @(Group-LoadAttempt -Attempts @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -Cause 'first' -LogLine 10)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -Cause 'second' -LogLine 20)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -Cause 'third' -LogLine 30)
        ))

        $loads.Count | Should -Be 1
        $loads[0].Attempts.Source | Should -Be @('campaign', 'campaign.bak', 'campaign.inprogress')
        $loads[0].Attempts.LogLine | Should -Be @(10, 20, 30)
    }

    It 'takes the cause of the first attempt, and the outcome of the last' {

        $load = Group-LoadAttempt -Attempts @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -Cause 'first')
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'L')
        )

        $load.Cause | Should -Be 'first'
        $load.Outcome | Should -Be 'L'
        $load.SaveFolder | Should -Be 'a'
    }

    It 'starts a new load once every source is used' {

        $loads = @(Group-LoadAttempt -Attempts @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -Cause 'one')
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -Cause 'two')
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -Cause 'three')
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -Cause 'four')
        ))

        $loads.Count | Should -Be 2
        $loads[1].Cause | Should -Be 'four'
    }

    It 'starts a new load after a loaded attempt, or on another save' {

        $loads = @(Group-LoadAttempt -Attempts @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'L')
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -Cause 'one')
            (New-Attempt -SavePath 'C:\saves\b' -Outcome 'F' -Cause 'two')
        ))

        $loads.SaveFolder | Should -Be @('a', 'a', 'b')
    }

    It 'returns nothing for no attempts' {
        @(Group-LoadAttempt -Attempts @()).Count | Should -Be 0
    }
}
