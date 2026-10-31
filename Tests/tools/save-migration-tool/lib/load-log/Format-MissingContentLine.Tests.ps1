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

        # ARRANGE
        $load = [pscustomobject]@{ SaveFolder = 'save_A'; Missing = $null }

        $expectedLine = 'the cause names no missing content'

        # ACT
        $actualLine = Format-MissingContentLine -Load $load

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }

    It 'names the kind and ID' {

        # ARRANGE
        $load = [pscustomobject]@{
            SaveFolder = 'save_A'
            Missing    = [pscustomobject]@{ Kind = 'Weapon'; Id = 'gun'; ReportedId = 'gun'; SpecClass = $null }
        }

        $expectedLine = 'missing Weapon <Content>gun'

        # ACT
        $actualLine = Format-MissingContentLine -Load $load

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }

    It 'adds the spec class and the reported ID when they say more' {

        # ARRANGE
        $load = [pscustomobject]@{
            SaveFolder = 'save_A'
            Missing    = [pscustomobject]@{ Kind = 'Spec'; Id = 'item'; ReportedId = 'item_named'; SpecClass = 'a.B' }
        }

        $expectedLine = 'missing Spec <Content>item of spec class <Code>a.B, named as <Content>item_named'

        # ACT
        $actualLine = Format-MissingContentLine -Load $load

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }
}
