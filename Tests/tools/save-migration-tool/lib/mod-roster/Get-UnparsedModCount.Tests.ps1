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

        $roster = @('P', 'U', 'P', 'U') | ForEach-Object { [pscustomobject]@{ Status = $_ } }

        Get-UnparsedModCount -Roster $roster | Should -Be 2
    }

    It 'counts none in an empty roster' {
        Get-UnparsedModCount -Roster @() | Should -Be 0
    }
}
