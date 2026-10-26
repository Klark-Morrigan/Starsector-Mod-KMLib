BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Format-ModDriftCounts.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Format-ModDriftLine.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-ModDriftChangeNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Format-ModDriftLines.ps1"
}

Describe 'Format-ModDriftLines' {

    BeforeEach {

        Mock Get-ModDriftChangeNames {
            [pscustomobject]@{ Unchanged = 'UNCH'; Changed = 'CHG'; Missing = 'MISS'; Added = 'ADD' }
        }

        Mock Format-ModDriftCounts { "counts of $(@($Rows).Count)" }
        Mock Format-ModDriftLine { "$($Row.Id) $($Row.Change)" }
    }

    It 'counts every row, then lists the changed, the missing and the added in turn, each in its order' {

        $rows = @(
            [pscustomobject]@{ Id = 'a'; Change = 'ADD' }
            [pscustomobject]@{ Id = 'b'; Change = 'UNCH' }
            [pscustomobject]@{ Id = 'c'; Change = 'MISS' }
            [pscustomobject]@{ Id = 'd'; Change = 'CHG' }
            [pscustomobject]@{ Id = 'e'; Change = 'CHG' }
        )

        Format-ModDriftLines -Rows $rows | Should -Be @(
            'Mod drift against the install: counts of 5'
            '  d CHG'
            '  e CHG'
            '  c MISS'
            '  a ADD'
        )
    }

    It 'writes the count line alone when no mod differs' {

        $rows = @([pscustomobject]@{ Id = 'b'; Change = 'UNCH' })

        Format-ModDriftLines -Rows $rows | Should -Be @('Mod drift against the install: counts of 1')
    }
}
