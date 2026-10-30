BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Get-ModStatusNames.ps1"
}

Describe 'Get-ModStatusNames' {

    It 'names the parsed and unparsed statuses' {

        # ARRANGE
        $expectedParsed = 'parsed'
        $expectedUnparsed = 'unparsed'

        # ACT
        $actualStatuses = Get-ModStatusNames

        # ASSERT
        $actualStatuses.Parsed | Should -Be $expectedParsed
        $actualStatuses.Unparsed | Should -Be $expectedUnparsed
    }
}
