BeforeAll {

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\load-log\Format-LoadDetailLines.ps1"
    . "$libRoot\load-log\Format-LoadOutcome.ps1"
    . "$libRoot\diagnosis\Format-DiagnosisReport.ps1"
}

Describe 'Format-DiagnosisReport' {

    BeforeEach {

        Mock Format-LoadOutcome { "<$Outcome>" }
        Mock Format-LoadDetailLines { ,@("  details of $($Load.SaveFolder)") }

        # Marks each painted value with its role, so a case pins the colour of every value it names.
        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ConsoleNumber { "<Number>$Number" }

        $script:Mods = @(
            [pscustomobject]@{ Id = 'alpha'; Name = ' Alpha'; Version = '1.40.0' }
            [pscustomobject]@{ Id = 'beta'; Name = 'Beta'; Version = '1.14.1aa' }
        )
    }

    It 'says no save is diagnosed when the report names none' {

        $report = [pscustomobject]@{ SaveFolder = $null; Load = $null; Mods = @() }

        Format-DiagnosisReport -Report $report | Should -Be @(
            ''
            'No load failed in the logs, so no save is diagnosed. Name one with <Code>-SaveFolder.'
        )
    }

    It 'reports the save, the newest load with its details, then how many mods the save holds, not the list' {

        $load = [pscustomobject]@{ SaveFolder = 'save_Test'; Outcome = 'Failed' }
        $report = [pscustomobject]@{ SaveFolder = 'save_Test'; Load = $load; Mods = $script:Mods }

        Format-DiagnosisReport -Report $report | Should -Be @(
            ''
            'Save: <Path>save_Test'
            'Newest load: <Failed>'
            '  details of save_Test'
            ''
            'Mods the save was last written with: <Number>2; <Script>Get-SaveModDrift.ps1 lists them'
        )
    }

    It 'says the logs hold no load of a named save' {

        $report = [pscustomobject]@{ SaveFolder = 'save_Test'; Load = $null; Mods = @() }

        Format-DiagnosisReport -Report $report | Should -Be @(
            ''
            'Save: <Path>save_Test'
            'Newest load: none in the logs'
            ''
            'Mods the save was last written with: <Number>0; <Script>Get-SaveModDrift.ps1 lists them'
        )
        Should -Invoke Format-LoadDetailLines -Times 0
    }
}
