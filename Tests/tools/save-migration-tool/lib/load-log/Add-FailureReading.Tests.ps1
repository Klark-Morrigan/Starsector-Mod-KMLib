BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\ConvertFrom-FailureTrace.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Get-MissingContent.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Add-FailureReading.ps1"

    function script:New-Attempt {
        # One attempt with empty readings, as Read-LoadLogFile files it.
        return [pscustomobject]@{ CauseType = $null; Cause = $null; Missing = $null; ObjectChain = @() }
    }
}

Describe 'Add-FailureReading' {

    BeforeEach {

        # The trace's lines, joined, stand for its cause; its first line stands for its object chain.
        Mock ConvertFrom-FailureTrace {
            [pscustomobject]@{ CauseType = 'T'; Cause = $Lines -join '/'; ObjectChain = @($Lines[0]) }
        }
        Mock Get-MissingContent { "missing from $CauseType $Cause" }
    }

    It 'reads each failed attempt''s trace into its cause, chain and missing content, and leaves the others empty' {

        $failed = New-Attempt
        $loaded = New-Attempt
        $load = [pscustomobject]@{ Attempts = @($failed, $loaded) }

        $traces = [System.Collections.Generic.Dictionary[object, System.Collections.Generic.List[string]]]::new(
            [System.Collections.Generic.ReferenceEqualityComparer]::Instance)
        $traces.Add($failed, [System.Collections.Generic.List[string]]@('first', 'second'))

        $result = Add-FailureReading -Load $load -TraceLinesByAttempt $traces

        $result | Should -Be $load
        $failed.CauseType | Should -Be 'T'
        $failed.Cause | Should -Be 'first/second'
        $failed.ObjectChain | Should -Be @('first')
        $failed.Missing | Should -Be 'missing from T first/second'
        $loaded.Cause | Should -BeNullOrEmpty
        $loaded.Missing | Should -BeNullOrEmpty

        Should -Invoke Get-MissingContent -Times 1 -Exactly
    }
}
