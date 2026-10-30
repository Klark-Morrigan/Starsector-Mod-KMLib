BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\readers\New-ParsedFileCache.ps1"
}

Describe 'New-ParsedFileCache' {

    It 'returns an empty <Property>' -ForEach @(
        @{ Property = 'JsonByPath' }
        @{ Property = 'SpreadsheetsByKey' }
    ) {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualCache = New-ParsedFileCache

        # ASSERT
        $actualCache.$Property.Count | Should -Be $expectedCount
    }

    # Windows finds a file whatever the case of its path, so one file's copy is parsed once.
    It 'matches <Property> keys ignoring case' -ForEach @(
        @{ Property = 'JsonByPath' }
        @{ Property = 'SpreadsheetsByKey' }
    ) {

        # ARRANGE
        $expectedComparer = [System.StringComparer]::OrdinalIgnoreCase

        # ACT
        $actualCache = New-ParsedFileCache

        # ASSERT
        $actualCache.$Property.Comparer | Should -Be $expectedComparer
    }
}
