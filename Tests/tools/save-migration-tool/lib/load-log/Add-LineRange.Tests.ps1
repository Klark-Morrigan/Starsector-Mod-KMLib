BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Add-LineRange.ps1"
}

Describe 'Add-LineRange' {

    BeforeEach {

        $script:Target = [System.Collections.Generic.List[string]]::new()
        $script:Target.Add('kept')
    }

    It 'appends the lines from one index to another, both included' {

        Add-LineRange -Target $script:Target -Lines @('a', 'b', 'c', 'd') -From 1 -To 2

        $script:Target | Should -Be @('kept', 'b', 'c')
    }

    It 'appends nothing for an empty range' {

        Add-LineRange -Target $script:Target -Lines @('a', 'b') -From 2 -To 1

        $script:Target | Should -Be @('kept')
    }
}
