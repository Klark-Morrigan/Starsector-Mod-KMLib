BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\New-LoadLogScan.ps1"
}

Describe 'New-LoadLogScan' {

    It 'starts with no attempt, no trace and nothing open' {

        # ARRANGE
        $expectedSaveBaseDirectory = 'C:\Games\starsector-core'
        $expectedAttemptCount = 0
        $expectedTraceCount = 0

        # ACT
        $actualScan = New-LoadLogScan -SaveBaseDirectory 'C:\Games\starsector-core'

        # ASSERT
        $actualScan.SaveBaseDirectory | Should -Be $expectedSaveBaseDirectory
        $actualScan.Attempts.Count | Should -Be $expectedAttemptCount
        $actualScan.TraceLinesByAttempt.Count | Should -Be $expectedTraceCount
        $actualScan.CurrentAttempt | Should -BeNullOrEmpty
        $actualScan.TraceLines | Should -BeNullOrEmpty
    }

    It 'files traces by attempt reference, so two attempts with equal fields keep apart' {

        # ARRANGE
        $scan = New-LoadLogScan -SaveBaseDirectory 'C:\'
        $first = [pscustomobject]@{ SavePath = 'C:\saves\a' }
        $second = [pscustomobject]@{ SavePath = 'C:\saves\a' }

        $scan.TraceLinesByAttempt.Add($first, [System.Collections.Generic.List[string]]@('one'))
        $scan.TraceLinesByAttempt.Add($second, [System.Collections.Generic.List[string]]@('two'))

        $expectedTraceLines = @('two')

        # ACT
        $actualTraceLines = $scan.TraceLinesByAttempt[$second]

        # ASSERT
        $actualTraceLines | Should -Be $expectedTraceLines
    }
}
