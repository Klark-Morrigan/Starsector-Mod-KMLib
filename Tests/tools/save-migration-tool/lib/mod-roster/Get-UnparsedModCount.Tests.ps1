BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Get-ModStatusNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Get-UnparsedModCount.ps1"
}

Describe 'Get-UnparsedModCount' {

    BeforeEach {
        Mock Get-ModStatusNames { [pscustomobject]@{ Parsed = 'P'; Unparsed = 'U' } }
    }

    It 'counts the mods whose status is not parsed' {

        # ARRANGE
        $roster = @('P', 'U', 'P', 'U') | ForEach-Object { [pscustomobject]@{ Status = $_ } }

        $expectedCount = 2

        # ACT
        $actualCount = Get-UnparsedModCount -Roster $roster

        # ASSERT
        $actualCount | Should -Be $expectedCount
    }

    It 'counts none in an empty roster' {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualCount = Get-UnparsedModCount -Roster @()

        # ASSERT
        $actualCount | Should -Be $expectedCount
    }
}
