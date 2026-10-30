BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Add-LineRange.ps1"
}

Describe 'Add-LineRange' {

    BeforeEach {

        $script:Target = [System.Collections.Generic.List[string]]::new()
        $script:Target.Add('kept')
    }

    It 'appends the lines from one index to another, both included' {

        # ARRANGE
        $expectedLines = @('kept', 'b', 'c')

        # ACT
        Add-LineRange -Target $script:Target -Lines @('a', 'b', 'c', 'd') -From 1 -To 2

        # ASSERT
        $script:Target | Should -Be $expectedLines
    }

    It 'appends nothing for an empty range' {

        # ARRANGE
        $expectedLines = @('kept')

        # ACT
        Add-LineRange -Target $script:Target -Lines @('a', 'b') -From 2 -To 1

        # ASSERT
        $script:Target | Should -Be $expectedLines
    }
}
