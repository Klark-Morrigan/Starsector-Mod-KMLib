BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\load-log\Format-LoadDetailLines.ps1"
    . "$libRoot\load-log\Format-LoadOutcome.ps1"
    . "$libRoot\mod-drift\Format-ModDriftLines.ps1"
    . "$libRoot\diagnosis\Format-DiagnosisReport.ps1"
}

Describe 'Format-DiagnosisReport' {

    BeforeEach {

        Mock Format-LoadOutcome { "<$Outcome>" }
        Mock Format-LoadDetailLines { ,@("  details of $($Load.SaveFolder)") }
        Mock Format-ModDriftLines {

            "drift of $(@($Rows).Count) mods"
            '  a drifted mod'
        }

        # Marks each painted value with its role, so a case pins the colour of every value it names.
        Mock Format-ConsoleText { "<$Role>$Text" }

        $script:ModDrift = @('alpha row', 'beta row')
    }

    It 'says no save is diagnosed when the report names none' {

        $report = [pscustomobject]@{ SaveFolder = $null; Load = $null; ModDrift = @() }

        Format-DiagnosisReport -Report $report | Should -Be @(
            ''
            'No load failed in the logs, so no save is diagnosed. Name one with <Code>-SaveFolder.'
        )
    }

    It 'reports the save, the newest load with its details, then the mod drift' {

        $load = [pscustomobject]@{ SaveFolder = 'save_Test'; Outcome = 'Failed' }
        $report = [pscustomobject]@{ SaveFolder = 'save_Test'; Load = $load; ModDrift = $script:ModDrift }

        Format-DiagnosisReport -Report $report | Should -Be @(
            ''
            'Save: <Path>save_Test'
            'Newest load: <Failed>'
            '  details of save_Test'
            ''
            'drift of 2 mods'
            '  a drifted mod'
        )
    }

    It 'says the logs hold no load of a named save' {

        $report = [pscustomobject]@{ SaveFolder = 'save_Test'; Load = $null; ModDrift = $script:ModDrift }

        Format-DiagnosisReport -Report $report | Should -Be @(
            ''
            'Save: <Path>save_Test'
            'Newest load: none in the logs'
            ''
            'drift of 2 mods'
            '  a drifted mod'
        )
        Should -Invoke Format-LoadDetailLines -Times 0
    }
}
