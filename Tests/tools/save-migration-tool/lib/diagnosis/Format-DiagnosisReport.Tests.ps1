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

        # ARRANGE
        $report = [pscustomobject]@{ SaveFolder = $null; Load = $null; ModDrift = @() }

        $expectedLines = @(
            ''
            'No load failed in the logs, so no save is diagnosed. Name one with <Code>-SaveFolder.'
        )

        # ACT
        $actualLines = Format-DiagnosisReport -Report $report

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'reports the save, the newest load with its details, then the mod drift' {

        # ARRANGE
        $load = [pscustomobject]@{ SaveFolder = 'save_Test'; Outcome = 'Failed' }
        $report = [pscustomobject]@{ SaveFolder = 'save_Test'; Load = $load; ModDrift = $script:ModDrift }

        $expectedLines = @(
            ''
            'Save: <Path>save_Test'
            'Newest load: <Failed>'
            '  details of save_Test'
            ''
            'drift of 2 mods'
            '  a drifted mod'
        )

        # ACT
        $actualLines = Format-DiagnosisReport -Report $report

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'says the logs hold no load of a named save' {

        # ARRANGE
        $report = [pscustomobject]@{ SaveFolder = 'save_Test'; Load = $null; ModDrift = $script:ModDrift }

        $expectedLines = @(
            ''
            'Save: <Path>save_Test'
            'Newest load: none in the logs'
            ''
            'drift of 2 mods'
            '  a drifted mod'
        )

        # ACT
        $actualLines = Format-DiagnosisReport -Report $report

        # ASSERT
        $actualLines | Should -Be $expectedLines
        Should -Invoke Format-LoadDetailLines -Times 0
    }

    # Counting nothing as "0 changed" would read as a save that matches the install.
    It 'says why the mods were not compared, rather than counting no drift' {

        # ARRANGE
        $report = [pscustomobject]@{
            SaveFolder      = 'save_Test'
            Load            = $null
            ModDrift        = $null
            ModDriftProblem = 'descriptor not found'
        }

        $expectedLastLine = 'Mod drift against the install: not compared, descriptor not found'

        # ACT
        $actualLastLine = Format-DiagnosisReport -Report $report | Select-Object -Last 1

        # ASSERT
        $actualLastLine | Should -Be $expectedLastLine
        Should -Invoke Format-ModDriftLines -Times 0
    }

    # An empty comparison is a real answer: neither side holds a mod.
    It 'counts an empty comparison as drift' {

        # ARRANGE
        $report = [pscustomobject]@{
            SaveFolder      = 'save_Test'
            Load            = $null
            ModDrift        = @()
            ModDriftProblem = $null
        }

        $expectedLastLines = @('drift of 0 mods', '  a drifted mod')

        # ACT
        $actualLastLines = Format-DiagnosisReport -Report $report | Select-Object -Last 2

        # ASSERT
        $actualLastLines | Should -Be $expectedLastLines
    }
}
