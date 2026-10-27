BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleNumber.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-ModDriftChangeNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Format-ModDriftCounts.ps1"
}

Describe 'Format-ModDriftCounts' {

    BeforeEach {

        Mock Get-ModDriftChangeNames {
            [pscustomobject]@{ Unchanged = 'Unch'; Changed = 'Chg'; Missing = 'Miss'; Added = 'Add' }
        }

        Mock Format-ConsoleNumber { "<Number>$Number" }
    }

    It 'counts each change, changed first and unchanged last, in lower case' {

        $rows = @('Unch', 'Chg', 'Unch', 'Add') | ForEach-Object { [pscustomobject]@{ Change = $_ } }

        Format-ModDriftCounts -Rows $rows |
            Should -Be '<Number>1 chg, <Number>0 miss, <Number>1 add, <Number>2 unch'
    }

    It 'counts no rows as none of each' {
        Format-ModDriftCounts -Rows @() | Should -Be '<Number>0 chg, <Number>0 miss, <Number>0 add, <Number>0 unch'
    }
}
