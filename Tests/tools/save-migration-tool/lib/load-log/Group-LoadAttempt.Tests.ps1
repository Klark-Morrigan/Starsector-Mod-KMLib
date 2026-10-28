BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$libRoot\Get-LoadOutcomeNames.ps1"
    . "$libRoot\Group-LoadAttempt.ps1"
    . "$libRoot\New-FailureReading.ps1"

    function script:New-Attempt {
        # One attempt with the fields Group-LoadAttempt reads.
        param(
            [string] $SavePath,
            [string] $Outcome,
            [int] $LogLine = 1
        )

        return [pscustomobject]@{ SavePath = $SavePath; Outcome = $Outcome; LogLine = $LogLine }
    }
}

Describe 'Group-LoadAttempt' {

    BeforeEach {

        Mock Get-LoadOutcomeNames { [pscustomobject]@{ Loaded = 'L'; Failed = 'F'; Unfinished = 'U' } }
        Mock New-FailureReading { [ordered]@{ Reading = 'empty' } }
    }

    It 'folds failed retries of one save into one load, naming each source' {

        # ARRANGE
        $attempts = @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 10)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 20)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 30)
        )

        $expectedLoadCount = 1
        $expectedSources = @('campaign', 'campaign.bak', 'campaign.inprogress')
        $expectedLogLines = @(10, 20, 30)

        # ACT
        $actualLoads = @(Group-LoadAttempt -Attempts $attempts)

        # ASSERT
        $actualLoads.Count | Should -Be $expectedLoadCount
        $actualLoads[0].Attempts.Source | Should -Be $expectedSources
        $actualLoads[0].Attempts.LogLine | Should -Be $expectedLogLines
    }

    It 'keeps the attempts it was handed, each with its source' {

        # ARRANGE
        $first = New-Attempt -SavePath 'C:\saves\a' -Outcome 'F'
        $attempts = @($first, (New-Attempt -SavePath 'C:\saves\a' -Outcome 'L'))

        $expectedSource = 'campaign'

        # ACT
        $actualLoad = Group-LoadAttempt -Attempts $attempts

        # ASSERT
        $actualLoad.Attempts[0] | Should -Be $first
        $first.Source | Should -Be $expectedSource
    }

    It 'takes the save of the first attempt and the outcome of the last' {

        # ARRANGE
        $attempts = @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F')
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'L')
        )

        $expectedSaveFolder = 'a'
        $expectedSavePath = 'C:\saves\a'
        $expectedOutcome = 'L'

        # ACT
        $actualLoad = Group-LoadAttempt -Attempts $attempts

        # ASSERT
        $actualLoad.SaveFolder | Should -Be $expectedSaveFolder
        $actualLoad.SavePath | Should -Be $expectedSavePath
        $actualLoad.Outcome | Should -Be $expectedOutcome
    }

    # The field order is the JSON's; Copy-FirstAttemptReading fills the readings later.
    It 'gives the load empty readings between its save fields and its attempts' {

        # ARRANGE
        $attempts = @(New-Attempt -SavePath 'C:\saves\a' -Outcome 'F')

        $expectedNames = @('SaveFolder', 'SavePath', 'Outcome', 'Reading', 'Attempts')
        $expectedReading = 'empty'

        # ACT
        $actualLoad = Group-LoadAttempt -Attempts $attempts

        # ASSERT
        $actualLoad.PSObject.Properties.Name | Should -Be $expectedNames
        $actualLoad.Reading | Should -Be $expectedReading
    }

    It 'starts a new load once every source is used' {

        # ARRANGE
        $attempts = @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 1)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 2)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 3)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F' -LogLine 4)
        )

        $expectedLoadCount = 2
        $expectedLogLines = @(4)

        # ACT
        $actualLoads = @(Group-LoadAttempt -Attempts $attempts)

        # ASSERT
        $actualLoads.Count | Should -Be $expectedLoadCount
        $actualLoads[1].Attempts.LogLine | Should -Be $expectedLogLines
    }

    # A player loading the same save again after it loaded, or after the game died mid-load, starts a load of their own.
    It 'starts a new load after a <Outcome> attempt of the same save, or on another save' -ForEach @(
        @{ Outcome = 'L' }
        @{ Outcome = 'U' }
    ) {

        # ARRANGE
        $attempts = @(
            (New-Attempt -SavePath 'C:\saves\a' -Outcome $Outcome)
            (New-Attempt -SavePath 'C:\saves\a' -Outcome 'F')
            (New-Attempt -SavePath 'C:\saves\b' -Outcome 'F')
        )

        $expectedSaveFolders = @('a', 'a', 'b')

        # ACT
        $actualLoads = @(Group-LoadAttempt -Attempts $attempts)

        # ASSERT
        $actualLoads.SaveFolder | Should -Be $expectedSaveFolders
    }

    It 'returns nothing for no attempts' {

        # ARRANGE
        $attempts = @()

        # ACT
        $actualLoads = @(Group-LoadAttempt -Attempts $attempts)

        # ASSERT
        $actualLoads.Count | Should -Be 0
    }
}
