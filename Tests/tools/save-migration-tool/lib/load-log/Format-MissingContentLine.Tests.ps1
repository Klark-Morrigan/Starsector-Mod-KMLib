BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Format-MissingContentLine.ps1"
}

Describe 'Format-MissingContentLine' {

    It 'says when the cause names no missing content' {
        Format-MissingContentLine -Load ([pscustomobject]@{ SaveFolder = 'save_A'; Missing = $null }) |
            Should -Be 'save_A: the cause names no missing content'
    }

    It 'names the kind and id' {

        $load = [pscustomobject]@{
            SaveFolder = 'save_A'
            Missing    = [pscustomobject]@{ Kind = 'Weapon'; Id = 'gun'; ReportedId = 'gun'; SpecClass = $null }
        }

        Format-MissingContentLine -Load $load | Should -Be 'save_A: missing Weapon gun'
    }

    It 'adds the spec class and the reported id when they say more' {

        $load = [pscustomobject]@{
            SaveFolder = 'save_A'
            Missing    = [pscustomobject]@{ Kind = 'Spec'; Id = 'item'; ReportedId = 'item_named'; SpecClass = 'a.B' }
        }

        Format-MissingContentLine -Load $load |
            Should -Be 'save_A: missing Spec item of spec class a.B, named as item_named'
    }
}
