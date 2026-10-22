BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\New-LoadLogScan.ps1"
}

Describe 'New-LoadLogScan' {

    It 'starts with no attempt, no trace and nothing open' {

        $scan = New-LoadLogScan -SaveBaseDirectory 'C:\Games\starsector-core'

        $scan.SaveBaseDirectory | Should -Be 'C:\Games\starsector-core'
        $scan.Attempts.Count | Should -Be 0
        $scan.TraceLinesByAttempt.Count | Should -Be 0
        $scan.CurrentAttempt | Should -BeNullOrEmpty
        $scan.TraceLines | Should -BeNullOrEmpty
    }

    It 'files traces by attempt reference, so two attempts with equal fields keep apart' {

        $scan = New-LoadLogScan -SaveBaseDirectory 'C:\'
        $first = [pscustomobject]@{ SavePath = 'C:\saves\a' }
        $second = [pscustomobject]@{ SavePath = 'C:\saves\a' }

        $scan.TraceLinesByAttempt.Add($first, [System.Collections.Generic.List[string]]@('one'))
        $scan.TraceLinesByAttempt.Add($second, [System.Collections.Generic.List[string]]@('two'))

        $scan.TraceLinesByAttempt[$second] | Should -Be @('two')
    }
}
