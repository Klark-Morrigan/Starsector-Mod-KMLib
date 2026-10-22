BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Format-MissingContentLine.ps1"
}

Describe 'Format-MissingContentLine' {

    BeforeEach {

        # Marks each painted value with its role, so a case pins the colour of every value it names.
        Mock Format-ConsoleText { "<$Role>$Text" }
    }

    It 'says when the cause names no missing content' {
        Format-MissingContentLine -Load ([pscustomobject]@{ SaveFolder = 'save_A'; Missing = $null }) |
            Should -Be 'the cause names no missing content'
    }

    It 'names the kind and ID' {

        $load = [pscustomobject]@{
            SaveFolder = 'save_A'
            Missing    = [pscustomobject]@{ Kind = 'Weapon'; Id = 'gun'; ReportedId = 'gun'; SpecClass = $null }
        }

        Format-MissingContentLine -Load $load | Should -Be 'missing Weapon <Content>gun'
    }

    It 'adds the spec class and the reported ID when they say more' {

        $load = [pscustomobject]@{
            SaveFolder = 'save_A'
            Missing    = [pscustomobject]@{ Kind = 'Spec'; Id = 'item'; ReportedId = 'item_named'; SpecClass = 'a.B' }
        }

        Format-MissingContentLine -Load $load |
            Should -Be 'missing Spec <Content>item of spec class <Code>a.B, named as <Content>item_named'
    }
}
