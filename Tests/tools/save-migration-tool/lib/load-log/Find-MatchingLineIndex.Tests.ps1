BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Find-MatchingLineIndex.ps1"
}

Describe 'Find-MatchingLineIndex' {

    It 'finds every line holding the text, in order, a repeated line at each of its places' {

        # ARRANGE
        $lines = @('a Loader line', 'other', 'a Loader line', 'Loader again')

        $expectedIndexes = @(0, 2, 3)

        # ACT
        $actualIndexes = Find-MatchingLineIndex -Lines $lines -Text 'Loader'

        # ASSERT
        $actualIndexes | Should -Be $expectedIndexes
    }

    It 'matches case-sensitively, and the text as plain text rather than a pattern' {

        # ARRANGE
        $lines = @('a.loader', 'aXLoader', 'a.Loader')

        $expectedIndexes = @(2)

        # ACT
        $actualIndexes = Find-MatchingLineIndex -Lines $lines -Text 'a.Loader'

        # ASSERT
        $actualIndexes | Should -Be $expectedIndexes
    }

    It 'finds nothing in <Description>' -ForEach @(
        @{ Description = 'lines without the text'; Lines = @('one', 'two') }
        @{ Description = 'no lines'; Lines = @() }
    ) {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualIndexes = @(Find-MatchingLineIndex -Lines $Lines -Text 'Loader')

        # ASSERT
        $actualIndexes.Count | Should -Be $expectedCount
    }
}
