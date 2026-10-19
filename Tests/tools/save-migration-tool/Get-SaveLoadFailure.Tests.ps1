# Pester tests for Get-SaveLoadFailure.ps1.
# Run with: Invoke-Pester -Path .\Get-SaveLoadFailure.Tests.ps1
#
# Each fixture under fixtures\ is a hand-written install root holding only starsector-core\starsector.log
# and its rotated siblings. The lines are trimmed copies of what 0.98a writes. No test reads a real log.

BeforeAll {

    # Tests\ mirrors the repo, so the script sits at the same relative path under the repo root.
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))

    $script:ScriptPath = Join-Path $repoRoot 'tools\save-migration-tool\Get-SaveLoadFailure.ps1'
    $script:FixturesRoot = Join-Path $PSScriptRoot 'fixtures'

    function script:Invoke-SaveLoadFailure {
        <#
            Runs the script in-process, so coverage sees it, with stderr swapped for a buffer.
            The script writes its diagnostics to the console's stderr, outside PowerShell's streams.
        #>
        param([hashtable] $Arguments)

        $stderrBuffer = [System.IO.StringWriter]::new()
        $originalStderr = [Console]::Error

        [Console]::SetError($stderrBuffer)

        try {
            $output = & $script:ScriptPath @Arguments

        } finally {
            [Console]::SetError($originalStderr)
        }

        return [pscustomobject]@{
            Output      = $output
            Diagnostics = @($stderrBuffer.ToString() -split "`r?`n" | Where-Object { $_ })
        }
    }

    function script:Get-FixtureRoot {
        param([string] $Name)

        return Join-Path $script:FixturesRoot $Name
    }
}

Describe 'Get-SaveLoadFailure' {

    Context 'two failed loads of one save, the second split across a log rotation' {

        BeforeAll {

            $script:FixtureRoot = Get-FixtureRoot 'two-failed-loads'
            $script:Run = Invoke-SaveLoadFailure @{ StarsectorRoot = $script:FixtureRoot }
            $script:Loads = @($script:Run.Output)
        }

        It 'lists each load once, newest first' {

            $script:Loads.Count | Should -Be 2
            $script:Loads[0].Cause | Should -Be 'Ship hull spec [BSC_Buffalo_Mk3_default_D] not found!'
            $script:Loads[1].Cause | Should -Be 'Weapon spec [tania_boomerang_launcher] not found!'
        }

        It 'groups the bak and inprogress retries under their load, in the order vanilla tries them' {

            $script:Loads[0].Outcome | Should -Be 'Failed'
            $script:Loads[0].Attempts.Source | Should -Be @('campaign', 'campaign.bak', 'campaign.inprogress')
            $script:Loads[0].Attempts.Outcome | Should -Be @('Failed', 'Failed', 'Failed')
        }

        It 'reads the root cause past a stack frame of the loader class' {

            $script:Loads[1].CauseType | Should -Be 'java.lang.RuntimeException'
            $script:Loads[1].Attempts[0].Cause | Should -Be 'Weapon spec [tania_boomerang_launcher] not found!'
        }

        It 'keeps the cause of each retry' {

            $script:Loads[0].Attempts[1].Cause | Should -Be 'Ship hull spec [BSC_Hammerhead_MK2_default_D] not found!'
            $script:Loads[0].Attempts[2].CauseType | Should -Be 'java.io.FileNotFoundException'
        }

        It 'places each attempt in the log file and line it starts on' {

            $attempts = $script:Loads[0].Attempts
            $attempts.LogFile | ForEach-Object { Split-Path -Leaf $_ } |
                Should -Be @('starsector.log.1', 'starsector.log', 'starsector.log')
            $attempts.LogLine | Should -Be @(2, 7, 15)
        }

        It 'names the save folder and resolves its path' {

            $script:Loads[1].SaveFolder | Should -Be 'save_Alpha_1'
            $script:Loads[1].SavePath | Should -Be 'C:\Games\Starsector\saves\save_Alpha_1'
        }

        It 'logs what it read and decided to stderr' {

            $script:Run.Diagnostics | Should -Be @(
                'Get-SaveLoadFailure: reading 3 log files, oldest first'
                "Get-SaveLoadFailure: read $script:FixtureRoot\starsector-core\starsector.log.2: 44 lines, 3 load attempts start here"
                "Get-SaveLoadFailure: read $script:FixtureRoot\starsector-core\starsector.log.1: 8 lines, 1 load attempts start here"
                "Get-SaveLoadFailure: read $script:FixtureRoot\starsector-core\starsector.log: 20 lines, 2 load attempts start here"
                'Get-SaveLoadFailure: grouped 6 attempts into 2 loads, 2 failed; listing them newest first'
                'Get-SaveLoadFailure: save_Alpha_1: missing Hull BSC_Buffalo_Mk3, named as BSC_Buffalo_Mk3_default_D'
                'Get-SaveLoadFailure: save_Alpha_1: missing Weapon tania_boomerang_launcher'
            )
        }

        It 'reads a missing weapon from the cause' {

            $script:Loads[1].Missing.Kind | Should -Be 'Weapon'
            $script:Loads[1].Missing.Id | Should -Be 'tania_boomerang_launcher'
            $script:Loads[1].Missing.ReportedId | Should -Be 'tania_boomerang_launcher'
        }

        It 'reads a missing d-hull as its base hull' {

            $script:Loads[0].Missing.Kind | Should -Be 'Hull'
            $script:Loads[0].Missing.Id | Should -Be 'BSC_Buffalo_Mk3'
            $script:Loads[0].Missing.ReportedId | Should -Be 'BSC_Buffalo_Mk3_default_D'
        }

        It 'reads the missing content of each retry, and none from a missing file' {

            $script:Loads[0].Attempts[1].Missing.Id | Should -Be 'BSC_Hammerhead_MK2'
            $script:Loads[0].Attempts[2].Missing | Should -BeNullOrEmpty
        }

        It 'prints the same loads as JSON with -AsJson' {

            $run = Invoke-SaveLoadFailure @{ StarsectorRoot = $script:FixtureRoot; AsJson = $true }
            $loads = @(($run.Output -join "`n") | ConvertFrom-Json)

            $loads.Count | Should -Be 2
            $loads[0].Attempts[1].Source | Should -Be 'campaign.bak'
            $loads[0].Attempts[1].LogLine | Should -Be 7
        }
    }

    Context 'a load that failed on the save and loaded from the bak' {

        BeforeAll {
            $script:Loads = @((Invoke-SaveLoadFailure @{ StarsectorRoot = (Get-FixtureRoot 'recovered-from-bak') }).Output)
        }

        It 'reports the load as loaded' {

            $script:Loads.Count | Should -Be 1
            $script:Loads[0].Outcome | Should -Be 'Loaded'
            $script:Loads[0].Attempts.Outcome | Should -Be @('Failed', 'Loaded')
            $script:Loads[0].Attempts.Source | Should -Be @('campaign', 'campaign.bak')
        }

        It 'keeps the cause the save failed on, from a trace with no Caused by' {

            $script:Loads[0].CauseType | Should -Be 'com.thoughtworks.xstream.mapper.CannotResolveClassException'
            $script:Loads[0].Cause | Should -Be 'data.scripts.removed.GoneScript'
        }

        It 'reads the unresolved class as missing content' {

            $script:Loads[0].Missing.Kind | Should -Be 'Class'
            $script:Loads[0].Missing.Id | Should -Be 'data.scripts.removed.GoneScript'
        }
    }

    Context 'one failed load per kind of missing content' {

        BeforeAll {

            $script:Run = Invoke-SaveLoadFailure @{ StarsectorRoot = (Get-FixtureRoot 'each-missing-kind') }
            $script:Loads = @($script:Run.Output)
        }

        It 'reads each spec store message into its kind and id' {

            $script:Loads[1..4].Missing.Kind | Should -Be @('Spec', 'Hullmod', 'FighterWing', 'Variant')
            $script:Loads[1..4].Missing.Id | Should -Be @('gone_item', 'gone_dmod', 'gone_wing', 'gone_hull_Standard')
        }

        It 'keeps the spec class of a generic spec, and none for another kind' {

            $script:Loads[1].Missing.SpecClass | Should -Be 'com.fs.starfarer.loading.SpecialItemSpec'
            $script:Loads[4].Missing.SpecClass | Should -BeNullOrEmpty
        }

        It 'reads no missing content from a cause that names none' {

            $script:Loads[0].CauseType | Should -Be 'java.lang.NullPointerException'
            $script:Loads[0].Missing | Should -BeNullOrEmpty
        }

        It 'logs what each cause was read as' {

            $script:Run.Diagnostics[-5..-1] | Should -Be @(
                'Get-SaveLoadFailure: save_Other_5: the cause names no missing content'
                'Get-SaveLoadFailure: save_Spec_4: missing Spec gone_item of spec class com.fs.starfarer.loading.SpecialItemSpec'
                'Get-SaveLoadFailure: save_Hullmod_3: missing Hullmod gone_dmod'
                'Get-SaveLoadFailure: save_Wing_2: missing FighterWing gone_wing'
                'Get-SaveLoadFailure: save_Variant_1: missing Variant gone_hull_Standard'
            )
        }
    }

    Context 'a clean load, then a load the log ends inside' {

        BeforeAll {

            $script:FixtureRoot = Get-FixtureRoot 'loaded-then-unfinished'
            $script:Loads = @((Invoke-SaveLoadFailure @{ StarsectorRoot = $script:FixtureRoot }).Output)
        }

        It 'reports the clean load with one attempt and no cause' {

            $script:Loads[1].Outcome | Should -Be 'Loaded'
            $script:Loads[1].Attempts.Count | Should -Be 1
            $script:Loads[1].Cause | Should -BeNullOrEmpty
        }

        It 'resolves a relative save path against the log folder' {
            $script:Loads[1].SavePath | Should -Be "$script:FixtureRoot\saves\save_Beta_2"
        }

        It 'reports the cut-off load as unfinished' {

            $script:Loads[0].SaveFolder | Should -Be 'save_Gamma_3'
            $script:Loads[0].Outcome | Should -Be 'Unfinished'
        }
    }

    Context 'a log with no loads' {

        It 'prints an empty JSON list and says it found nothing' {

            $run = Invoke-SaveLoadFailure @{ StarsectorRoot = (Get-FixtureRoot 'no-loads'); AsJson = $true }

            $run.Output | Should -Be '[]'
            $run.Diagnostics[-1] | Should -Be 'Get-SaveLoadFailure: grouped 0 attempts into 0 loads, 0 failed; listing them newest first'
        }
    }

    Context 'a missing log' {

        It 'fails naming the path' {

            $missingLog = Join-Path $TestDrive 'starsector.log'

            { Invoke-SaveLoadFailure @{ LogPath = $missingLog } } | Should -Throw "log not found: $missingLog"
        }
    }
}
