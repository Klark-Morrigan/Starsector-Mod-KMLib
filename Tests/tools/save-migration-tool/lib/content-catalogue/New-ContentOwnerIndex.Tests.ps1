BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\New-ContentOwnerIndex.ps1"
}

Describe 'New-ContentOwnerIndex' {

    It 'returns an empty index' {

        # ARRANGE
        $expectedType = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]]
        $expectedCount = 0

        # ACT
        $actualIndex = New-ContentOwnerIndex

        # ASSERT
        $actualIndex | Should -BeOfType $expectedType
        $actualIndex.Count | Should -Be $expectedCount
    }

    # The game matches content IDs case-sensitively, so two IDs differing in case are two pieces of content.
    It 'keeps IDs that differ only in case apart' {

        # ARRANGE
        $index = New-ContentOwnerIndex
        $index.Add('hull', [System.Collections.Generic.List[string]]::new())

        $expectedHasOtherCase = $false

        # ACT
        $actualHasOtherCase = $index.ContainsKey('Hull')

        # ASSERT
        $actualHasOtherCase | Should -Be $expectedHasOtherCase
    }

    # Each reader fills its own index, so one reader's IDs must never show through another's.
    It 'returns a new index on each call' {

        # ARRANGE
        $filledIndex = New-ContentOwnerIndex
        $filledIndex.Add('filled', [System.Collections.Generic.List[string]]::new())

        $expectedCount = 0

        # ACT
        $actualIndex = New-ContentOwnerIndex

        # ASSERT
        $actualIndex.Count | Should -Be $expectedCount
    }
}
