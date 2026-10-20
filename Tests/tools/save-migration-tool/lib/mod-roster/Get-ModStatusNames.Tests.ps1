BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Get-ModStatusNames.ps1"
}

Describe 'Get-ModStatusNames' {

    It 'names the parsed and unparsed statuses' {

        $statuses = Get-ModStatusNames

        $statuses.Parsed | Should -Be 'ok'
        $statuses.Unparsed | Should -Be 'unparsed'
    }
}
