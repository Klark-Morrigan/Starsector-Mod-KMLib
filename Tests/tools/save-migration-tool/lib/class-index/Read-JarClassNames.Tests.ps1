BeforeAll {

    . "$PSScriptRoot\..\..\helpers\New-TestJar.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index\Read-JarClassNames.ps1"
}

Describe 'Read-JarClassNames' {

    It 'names each class entry as Java does, nested classes included' {

        $jar = New-TestJar -Path (Join-Path $TestDrive 'classes.jar') -EntryNames @(
            'data/scripts/Plugin.class'
            'data/scripts/Plugin$Inner.class'
        )

        Read-JarClassNames -Path $jar | Should -Be @('data.scripts.Plugin', 'data.scripts.Plugin$Inner')
    }

    It 'skips folders, resources and the classes under META-INF' {

        $jar = New-TestJar -Path (Join-Path $TestDrive 'mixed.jar') -EntryNames @(
            'data/'
            'data/config/settings.json'
            'META-INF/MANIFEST.MF'
            'META-INF/versions/9/module-info.class'
            'data/Kept.class'
        )

        Read-JarClassNames -Path $jar | Should -Be @('data.Kept')
    }

    It 'returns no names for a jar with no class' {

        $jar = New-TestJar -Path (Join-Path $TestDrive 'empty.jar') -EntryNames @('readme.txt')

        @(Read-JarClassNames -Path $jar).Count | Should -Be 0
    }
}
