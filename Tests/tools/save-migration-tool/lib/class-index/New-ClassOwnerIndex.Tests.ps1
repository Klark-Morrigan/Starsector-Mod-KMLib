BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index"

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\Get-CoreOwnerName.ps1"
    . "$libRoot\New-ClassOwnerIndex.ps1"
    . "$libRoot\Select-JarClassNames.ps1"

    function script:New-Source {
        # One owner and its JARs, as the index takes them.
        param(
            [string] $Owner,
            [string[]] $Jars
        )

        return [pscustomobject]@{ Owner = $Owner; Jars = $Jars }
    }
}

Describe 'New-ClassOwnerIndex' {

    BeforeEach {

        Mock Get-CoreOwnerName { 'core' }

        # Each JAR's file name lists its classes: a.B+c.D.jar holds a.B and c.D. A JAR named broken.jar throws.
        # The JAR files exist, empty, so the index finds them on disk; their reading is mocked.
        Mock Select-JarClassNames {

            if ($Path.EndsWith('broken.jar')) {
                throw 'End of Central Directory record could not be found.'
            }

            $heldClasses = [System.IO.Path]::GetFileNameWithoutExtension($Path) -split '\+'

            $ClassNames | Where-Object { $_ -cin $heldClasses }
        }
    }

    It 'names the owner of each wanted class in every JAR of a source, and leaves the other classes out' {

        $index = New-ClassOwnerIndex -ClassNames @('a.One', 'a.Two') -Sources @(
            New-Source -Owner 'alpha' -Jars @(
                (New-TestFile -Name 'a.One+a.Unwanted.jar')
                (New-TestFile -Name 'a.Two.jar')
            )
        )

        @($index.OwnersByClass['a.One']) | Should -Be @('alpha')
        @($index.OwnersByClass['a.Two']) | Should -Be @('alpha')
        $index.OwnersByClass.ContainsKey('a.Unwanted') | Should -BeFalse
        $index.ModJarCount | Should -Be 2

        Should -Invoke Select-JarClassNames -ParameterFilter { "$ClassNames" -eq 'a.One a.Two' } -Times 2 -Exactly
    }

    # A caller reports the mods' JARs against the mods, and the core's against the classpath it listed.
    It 'counts the JARs read for the mods apart from those read for the core' {

        $index = New-ClassOwnerIndex -ClassNames @('x.A') -Sources @(
            New-Source -Owner 'core' -Jars @((New-TestFile -Name 'core\a.jar'), (New-TestFile -Name 'core\b.jar'))
            New-Source -Owner 'alpha' -Jars @(New-TestFile -Name 'alpha\c.jar')
        )

        $index.CoreJarCount | Should -Be 2
        $index.ModJarCount | Should -Be 1
    }

    It 'keeps every mod owning a class two mods hold, in source order, and counts it as shared' {

        $index = New-ClassOwnerIndex -ClassNames @('x.Shared') -Sources @(
            New-Source -Owner 'alpha' -Jars @(New-TestFile -Name 'alpha\x.Shared.jar')
            New-Source -Owner 'beta' -Jars @(New-TestFile -Name 'beta\x.Shared.jar')
        )

        @($index.OwnersByClass['x.Shared']) | Should -Be @('alpha', 'beta')
        $index.SharedClassCount | Should -Be 1
        $index.ShadowedClassCount | Should -Be 0
    }

    It 'keeps the core as the only owner of a class a mod also holds, whichever source comes first' {

        $index = New-ClassOwnerIndex -ClassNames @('x.Engine') -Sources @(
            New-Source -Owner 'alpha' -Jars @(New-TestFile -Name 'alpha\shadow\x.Engine.jar')
            New-Source -Owner 'core' -Jars @(New-TestFile -Name 'core\x.Engine.jar')
        )

        @($index.OwnersByClass['x.Engine']) | Should -Be @('core')
        $index.ShadowedClassCount | Should -Be 1
        $index.SharedClassCount | Should -Be 0
    }

    It 'names an owner once when two of its JARs hold the same class' {

        $index = New-ClassOwnerIndex -ClassNames @('x.Dup') -Sources @(
            New-Source -Owner 'alpha' -Jars @(
                (New-TestFile -Name 'one\x.Dup.jar')
                (New-TestFile -Name 'two\x.Dup.jar')
            )
        )

        @($index.OwnersByClass['x.Dup']) | Should -Be @('alpha')
        $index.SharedClassCount | Should -Be 0
    }

    It 'skips a JAR that is not on disk, and one no zip reader opens, returning each with its reason' {

        $missing = Join-Path $TestDrive 'gone.jar'
        $broken = New-TestFile -Name 'broken.jar'

        $index = New-ClassOwnerIndex -ClassNames @('x.Kept') -Sources @(
            New-Source -Owner 'alpha' -Jars @($missing, $broken, (New-TestFile -Name 'kept\x.Kept.jar'))
        )

        $index.SkippedJars.Path | Should -Be @($missing, $broken)
        $index.SkippedJars.Reason | Should -Be @('not on disk', 'End of Central Directory record could not be found.')
        $index.ModJarCount | Should -Be 1
        @($index.OwnersByClass['x.Kept']) | Should -Be @('alpha')
    }
}
