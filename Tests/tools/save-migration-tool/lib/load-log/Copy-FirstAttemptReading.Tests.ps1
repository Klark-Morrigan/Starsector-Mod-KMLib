BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Copy-FirstAttemptReading.ps1"
}

Describe 'Copy-FirstAttemptReading' {

    It 'gives the load its first attempt''s cause, missing content and chain, not a retry''s' {

        $load = [pscustomobject]@{
            CauseType   = $null
            Cause       = $null
            Missing     = $null
            ObjectChain = @()
            Attempts    = @(
                [pscustomobject]@{ CauseType = 'T1'; Cause = 'first'; Missing = 'm1'; ObjectChain = @('c1') }
                [pscustomobject]@{ CauseType = 'T2'; Cause = 'second'; Missing = 'm2'; ObjectChain = @('c2') }
            )
        }

        $result = Copy-FirstAttemptReading -Load $load

        $result | Should -Be $load
        $load.CauseType | Should -Be 'T1'
        $load.Cause | Should -Be 'first'
        $load.Missing | Should -Be 'm1'
        $load.ObjectChain | Should -Be @('c1')
    }
}
