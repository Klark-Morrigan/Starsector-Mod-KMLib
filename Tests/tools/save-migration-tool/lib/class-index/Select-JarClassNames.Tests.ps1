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

        # ARRANGE
        $expectedClassNames = @('a.b.Outer$Inner', 'a.b.Held')

        # ACT
        $actualClassNames = Select-JarClassNames -Path $script:Jar `
            -ClassNames @('a.b.Outer$Inner', 'a.b.Absent', 'a.b.Held')

        # ASSERT
        $actualClassNames | Should -Be $expectedClassNames
    }

    It 'compares class names by case, as Java does' {

        # ACT
        $actualClassNames = @(Select-JarClassNames -Path $script:Jar -ClassNames @('a.b.held'))

        # ASSERT
        $actualClassNames.Count | Should -Be 0
    }

    It 'keeps none from a JAR with no entries' {

        # ARRANGE
        $emptyJar = New-TestJar -Path (Join-Path $TestDrive 'empty.jar')

        # ACT
        $actualClassNames = @(Select-JarClassNames -Path $emptyJar -ClassNames @('a.b.Held'))

        # ASSERT
        $actualClassNames.Count | Should -Be 0
    }

    It 'fails on a file no zip reader opens' {

        # ARRANGE
        $notAJar = Join-Path $TestDrive 'broken.jar'

        [System.IO.File]::WriteAllText($notAJar, 'not a zip')

        # ACT
        $actualCall = { Select-JarClassNames -Path $notAJar -ClassNames @('a.b.Held') }

        # ASSERT
        $actualCall | Should -Throw
    }
}
