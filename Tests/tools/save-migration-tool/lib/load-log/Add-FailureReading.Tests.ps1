BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$libRoot\Add-FailureReading.ps1"
    . "$libRoot\ConvertFrom-FailureTrace.ps1"
    . "$libRoot\Get-MissingContent.ps1"
    . "$libRoot\New-FailureReading.ps1"

    function script:New-Attempt {
        # One attempt holding only its empty readings, as the mocked New-FailureReading names them.
        return [pscustomobject](New-FailureReading)
    }

    function script:New-TraceIndex {
        # Each given attempt's trace lines, keyed by the attempt itself, as Read-LoadAttempt files them.
        param([System.Collections.IDictionary] $LinesByAttempt)

        $traces = [System.Collections.Generic.Dictionary[object, System.Collections.Generic.List[string]]]::new(
            [System.Collections.Generic.ReferenceEqualityComparer]::Instance)

        foreach ($attempt in $LinesByAttempt.Keys) {
            $traces.Add($attempt, [System.Collections.Generic.List[string]]$LinesByAttempt[$attempt])
        }

        return ,$traces
    }
}

Describe 'Add-FailureReading' {

    BeforeEach {

        Mock New-FailureReading { [ordered]@{ CauseType = $null; Cause = $null; Missing = $null; ObjectChain = @() } }

        # The trace's lines, joined, stand for its cause; its first line stands for its object chain.
        Mock ConvertFrom-FailureTrace {
            [pscustomobject]@{ CauseType = 'T'; Cause = $Lines -join '/'; ObjectChain = @($Lines[0]) }
        }
        Mock Get-MissingContent { "missing from $CauseType $Cause" }
    }

    It 'reads a failed attempt''s trace into its cause, chain and missing content' {

        # ARRANGE
        $failed = New-Attempt
        $load = [pscustomobject]@{ Attempts = @($failed) }
        $traces = New-TraceIndex -LinesByAttempt @{ $failed = @('first', 'second') }

        $expectedCauseType = 'T'
        $expectedCause = 'first/second'
        $expectedObjectChain = @('first')
        $expectedMissing = 'missing from T first/second'

        # ACT
        $actualLoad = Add-FailureReading -Load $load -TraceLinesByAttempt $traces

        # ASSERT
        $actualLoad | Should -Be $load
        $failed.CauseType | Should -Be $expectedCauseType
        $failed.Cause | Should -Be $expectedCause
        $failed.ObjectChain | Should -Be $expectedObjectChain
        $failed.Missing | Should -Be $expectedMissing
    }

    # Only a failed attempt has a trace; a loaded or unfinished one has no cause to read.
    It 'leaves an attempt with no trace empty' {

        # ARRANGE
        $failed = New-Attempt
        $loaded = New-Attempt
        $load = [pscustomobject]@{ Attempts = @($failed, $loaded) }
        $traces = New-TraceIndex -LinesByAttempt @{ $failed = @('first') }

        # ACT
        Add-FailureReading -Load $load -TraceLinesByAttempt $traces | Out-Null

        # ASSERT
        $loaded.CauseType | Should -BeNullOrEmpty
        $loaded.Cause | Should -BeNullOrEmpty
        $loaded.Missing | Should -BeNullOrEmpty
        $loaded.ObjectChain.Count | Should -Be 0

        Should -Invoke ConvertFrom-FailureTrace -Times 1 -Exactly
        Should -Invoke Get-MissingContent -Times 1 -Exactly
    }
}
