BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\New-KeyedFileSource.ps1"
}

Describe 'New-KeyedFileSource' {

    It 'reads the top-level keys, every one of them, by default' {

        $source = New-KeyedFileSource -Path 'data/config/planets.json'

        $source.Path | Should -Be 'data/config/planets.json'
        $source.NestedKey | Should -BeNullOrEmpty
        $source.ExcludedKeys.Count | Should -Be 0
        $source.IsObjectEntryOnly | Should -BeFalse
    }

    It 'keeps a nested key, the keys left out, and whether only object entries count' {

        $source = New-KeyedFileSource -Path 's.json' -NestedKey 'music' -ExcludedKeys @('x') -IsObjectEntryOnly

        $source.NestedKey | Should -Be 'music'
        $source.ExcludedKeys | Should -Be @('x')
        $source.IsObjectEntryOnly | Should -BeTrue
    }
}
