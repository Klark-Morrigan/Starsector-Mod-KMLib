BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\console\Format-ConsoleList.ps1"
}

Describe 'Format-ConsoleList' {

    BeforeEach {
        Mock Format-ConsoleText { "<$Role>$Text" }
    }

    It 'paints each value in the role, in order, and joins them with commas' {

        # ARRANGE
        $expectedList = '<Mod>nexerelin, <Mod>starlords'

        # ACT
        $actualList = Format-ConsoleList -Role 'Mod' -Values @('nexerelin', 'starlords')

        # ASSERT
        $actualList | Should -Be $expectedList
    }

    It 'writes one value alone' {

        # ARRANGE
        $expectedList = '<Path>loose-assets'

        # ACT
        $actualList = Format-ConsoleList -Role 'Path' -Values @('loose-assets')

        # ASSERT
        $actualList | Should -Be $expectedList
    }

    # A missing list must not paint one empty value, which would leave colour codes around nothing.
    It 'paints nothing for a missing list' {

        # ARRANGE
        $expectedList = ''

        # ACT
        $actualList = Format-ConsoleList -Role 'Mod' -Values $null

        # ASSERT
        $actualList | Should -Be $expectedList
        Should -Invoke Format-ConsoleText -Times 0
    }
}
