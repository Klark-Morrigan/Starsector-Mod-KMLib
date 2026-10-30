BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content-catalogue\definitions\New-KeyedFileSource.ps1"
}

Describe 'New-KeyedFileSource' {

    It 'reads the top-level keys, every one of them, by default' {

        # ARRANGE
        $expectedPath = 'data/config/planets.json'
        $expectedExcludedKeyCount = 0

        # ACT
        $actualSource = New-KeyedFileSource -Path 'data/config/planets.json'

        # ASSERT
        $actualSource.Path | Should -Be $expectedPath
        $actualSource.NestedKey | Should -BeNullOrEmpty
        $actualSource.ExcludedKeys.Count | Should -Be $expectedExcludedKeyCount
        $actualSource.IsObjectEntryOnly | Should -BeFalse
    }

    It 'keeps a nested key, the keys left out, and whether only object entries count' {

        # ARRANGE
        $expectedNestedKey = 'music'
        $expectedExcludedKeys = @('x')

        # ACT
        $actualSource = New-KeyedFileSource -Path 's.json' -NestedKey 'music' -ExcludedKeys @('x') -IsObjectEntryOnly

        # ASSERT
        $actualSource.NestedKey | Should -Be $expectedNestedKey
        $actualSource.ExcludedKeys | Should -Be $expectedExcludedKeys
        $actualSource.IsObjectEntryOnly | Should -BeTrue
    }
}
