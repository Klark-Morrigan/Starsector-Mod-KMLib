BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Find-MatchingLineIndex.ps1"
}

Describe 'Find-MatchingLineIndex' {

    It 'finds every line holding the text, in order, a repeated line at each of its places' {

        $lines = @('a Loader line', 'other', 'a Loader line', 'Loader again')

        Find-MatchingLineIndex -Lines $lines -Text 'Loader' | Should -Be @(0, 2, 3)
    }

    It 'matches case-sensitively, and the text as plain text rather than a pattern' {

        $lines = @('a.loader', 'aXLoader', 'a.Loader')

        Find-MatchingLineIndex -Lines $lines -Text 'a.Loader' | Should -Be @(2)
    }

    It 'finds nothing in <Description>' -ForEach @(
        @{ Description = 'lines without the text'; Lines = @('one', 'two') }
        @{ Description = 'no lines'; Lines = @() }
    ) {
        @(Find-MatchingLineIndex -Lines $Lines -Text 'Loader').Count | Should -Be 0
    }
}
