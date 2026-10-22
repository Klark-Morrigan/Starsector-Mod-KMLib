BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-TestJar.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index\Select-JarClassNames.ps1"
}

# A JAR is a zip archive, so each case writes a real one: the lookup is the zip reader's.
Describe 'Select-JarClassNames' {

    BeforeAll {

        $script:Jar = New-TestJar -Path (Join-Path $TestDrive 'mod.jar') `
            -EntryNames @('data/config/settings.json', 'META-INF/versions/9/a/b/Held.class') `
            -ClassNames @('a.b.Held', 'a.b.Outer$Inner')
    }

    It 'keeps the wanted classes the JAR holds, an inner class included, in the order asked' {

        Select-JarClassNames -Path $script:Jar -ClassNames @('a.b.Outer$Inner', 'a.b.Absent', 'a.b.Held') |
            Should -Be @('a.b.Outer$Inner', 'a.b.Held')
    }

    It 'compares class names by case, as Java does' {
        @(Select-JarClassNames -Path $script:Jar -ClassNames @('a.b.held')).Count | Should -Be 0
    }

    It 'keeps none from a JAR with no entries' {

        $emptyJar = New-TestJar -Path (Join-Path $TestDrive 'empty.jar')

        @(Select-JarClassNames -Path $emptyJar -ClassNames @('a.b.Held')).Count | Should -Be 0
    }

    It 'fails on a file no zip reader opens' {

        $notAJar = Join-Path $TestDrive 'broken.jar'

        [System.IO.File]::WriteAllText($notAJar, 'not a zip')

        { Select-JarClassNames -Path $notAJar -ClassNames @('a.b.Held') } | Should -Throw
    }
}
