BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Get-LoadOutcomeNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Group-LoadAttempt.ps1"

    function script:New-Attempt {
        # One attempt as Read-LoadLogFile files it.
        param(
            [string] $SavePath,
            [string] $Outcome,
            [int] $LogLine = 1
        )

        return [pscustomobject]@{
            SavePath    = $SavePath
            Outcome     = $Outcome
            CauseType   = $null
            Cause       = $null
            Missing     = $null
            ObjectChain = @()
            LogFile     = 'C:\starsector.log'
            LogLine     = $LogLine
        }
    }
}

Describe 'Group-LoadAttempt' {

    BeforeEach {
        Mock Get-LoadOutcomeNames { [pscustomobject]@{ Loaded = 'L'; Failed = 'F'; Unfinished = 'U' } }
    }

    It 'folds failed retries of one save into one load, naming each source' {

        $loads = @(Group-LoadAttempt -Attempts @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 10)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 20)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 30)
        ))

        $loads.Count | Should -Be 1
        $loads[0].Attempts.Source | Should -Be @('campaign', 'campaign.bak', 'campaign.inprogress')
        $loads[0].Attempts.LogLine | Should -Be @(10, 20, 30)
    }

    It 'keeps the attempts it was handed, each with its source' {

        $first = New-Attempt -SavePath 'C:\saves\a' -Outcome 'F'

        $load = Group-LoadAttempt -Attempts @($first, (New-Attempt -SavePath 'C:\saves\a' -Outcome 'L'))

        $load.Attempts[0] | Should -Be $first
        $first.Source | Should -Be 'campaign'
    }

    It 'takes the save of the first attempt and the outcome of the last, leaving the readings empty' {

        $load = Group-LoadAttempt -Attempts @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F')
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'L')
        )

        $load.SaveFolder | Should -Be 'a'
        $load.SavePath | Should -Be 'C:\saves\a'
        $load.Outcome | Should -Be 'L'
        $load.CauseType | Should -BeNullOrEmpty
        $load.Cause | Should -BeNullOrEmpty
        $load.Missing | Should -BeNullOrEmpty
        $load.ObjectChain.Count | Should -Be 0
    }

    It 'starts a new load once every source is used' {

        $loads = @(Group-LoadAttempt -Attempts @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 1)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 2)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 3)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 4)
        ))

        $loads.Count | Should -Be 2
        $loads[1].Attempts.LogLine | Should -Be @(4)
    }

    It 'starts a new load after a <Outcome> attempt of the same save, or on another save' -ForEach @(
        @{ Outcome = 'L' }
        @{ Outcome = 'U' }
    ) {
        $loads = @(Group-LoadAttempt -Attempts @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome $Outcome)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F')
            (New-Attempt -SavePath 'C:\saves\b' -Outcome 'F')
        ))

        $loads.SaveFolder | Should -Be @('a', 'a', 'b')
    }

    It 'returns nothing for no attempts' {
        @(Group-LoadAttempt -Attempts @()).Count | Should -Be 0
    }
}
