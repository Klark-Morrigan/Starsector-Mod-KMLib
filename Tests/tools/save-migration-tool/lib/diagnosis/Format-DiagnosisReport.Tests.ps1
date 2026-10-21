BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Format-MissingContentLine.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Format-ObjectChainLine.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\diagnosis\Format-DiagnosisReport.ps1"
}

Describe 'Format-DiagnosisReport' {

    BeforeEach {

        Mock Format-MissingContentLine { "missing line for $($Load.SaveFolder)" }
        Mock Format-ObjectChainLine { "chain line for $($Load.SaveFolder)" }

        $script:Mods = @(
            [pscustomobject]@{ Id = 'alpha'; Name = ' Alpha'; Version = '1.40.0' }
            [pscustomobject]@{ Id = 'beta'; Name = 'Beta'; Version = '1.14.1aa' }
        )
    }

    It 'says no save is diagnosed when the report names none' {

        $report = [pscustomobject]@{ SaveFolder = $null; Load = $null; Mods = @() }

        Format-DiagnosisReport -Report $report |
            Should -Be @('No load failed in the logs, so no save is diagnosed. Name one with -SaveFolder.')
    }

    It 'reports a failed load''s cause and readings, then how many mods the save holds, not the list' {

        $load = [pscustomobject]@{
            SaveFolder  = 'save_Test'
            Outcome     = 'Failed'
            CauseType   = 'java.lang.RuntimeException'
            Cause       = 'Weapon spec [w] not found!'
            ObjectChain = @([pscustomobject]@{ Depth = 0 })
        }

        $report = [pscustomobject]@{ SaveFolder = 'save_Test'; Load = $load; Mods = $script:Mods }

        Format-DiagnosisReport -Report $report | Should -Be @(
            'Save: save_Test'
            'Newest load: Failed'
            '  Cause: java.lang.RuntimeException: Weapon spec [w] not found!'
            '  missing line for save_Test'
            '  chain line for save_Test'
            ''
            'Mods the save was last written with: 2; Get-SaveModDrift.ps1 lists them'
        )
    }

    It 'reports a load with no cause and no chain by its outcome alone' {

        $load = [pscustomobject]@{ SaveFolder = 'save_Test'; Outcome = 'Loaded'; CauseType = $null; ObjectChain = @() }
        $report = [pscustomobject]@{ SaveFolder = 'save_Test'; Load = $load; Mods = @() }

        Format-DiagnosisReport -Report $report | Should -Be @(
            'Save: save_Test'
            'Newest load: Loaded'
            ''
            'Mods the save was last written with: 0; Get-SaveModDrift.ps1 lists them'
        )

        Should -Invoke Format-MissingContentLine -Times 0
        Should -Invoke Format-ObjectChainLine -Times 0
    }

    It 'says the logs hold no load of a named save' {

        $report = [pscustomobject]@{ SaveFolder = 'save_Test'; Load = $null; Mods = $script:Mods }

        (Format-DiagnosisReport -Report $report)[1] | Should -Be 'Newest load: none in the logs'
    }
}
