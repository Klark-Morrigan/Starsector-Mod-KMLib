BeforeAll {

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index"

    . "$libRoot\Get-CoreOwnerName.ps1"
    . "$libRoot\New-ClassOwnerIndex.ps1"
    . "$libRoot\Read-JarClassNames.ps1"

    function script:New-JarFile {
        # Writes an empty file under TestDrive for a jar path to exist; the jar reader is mocked.
        param([string] $Name)

        $path = Join-Path $TestDrive $Name

        New-Item -ItemType File -Path $path -Force | Out-Null

        return $path
    }

    function script:New-Source {
        # One owner and its jars, as the index takes them.
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

        # Each jar's file name lists its classes: a.B+c.D.jar holds a.B and c.D. A jar named broken.jar throws.
        Mock Read-JarClassNames {

            if ($Path.EndsWith('broken.jar')) {
                throw 'End of Central Directory record could not be found.'
            }

            [System.IO.Path]::GetFileNameWithoutExtension($Path) -split '\+'
        }
    }

    It 'names the owner of each wanted class in every jar of a source, and leaves the other classes out' {

        $index = New-ClassOwnerIndex -ClassNames @('a.One', 'a.Two') -Sources @(
            New-Source -Owner 'alpha' -Jars @((New-JarFile 'a.One+a.Unwanted.jar'), (New-JarFile 'a.Two.jar'))
        )

        @($index.OwnersByClass['a.One']) | Should -Be @('alpha')
        @($index.OwnersByClass['a.Two']) | Should -Be @('alpha')
        $index.OwnersByClass.ContainsKey('a.Unwanted') | Should -BeFalse
        $index.JarCount | Should -Be 2
    }

    It 'keeps every mod owning a class two mods hold, in source order, and counts it as shared' {

        $index = New-ClassOwnerIndex -ClassNames @('x.Shared') -Sources @(
            New-Source -Owner 'alpha' -Jars @(New-JarFile 'alpha\x.Shared.jar')
            New-Source -Owner 'beta' -Jars @(New-JarFile 'beta\x.Shared.jar')
        )

        @($index.OwnersByClass['x.Shared']) | Should -Be @('alpha', 'beta')
        $index.SharedClassCount | Should -Be 1
        $index.ShadowedClassCount | Should -Be 0
    }

    It 'keeps the core as the only owner of a class a mod also holds, whichever source comes first' {

        $index = New-ClassOwnerIndex -ClassNames @('x.Engine') -Sources @(
            New-Source -Owner 'alpha' -Jars @(New-JarFile 'alpha\shadow\x.Engine.jar')
            New-Source -Owner 'core' -Jars @(New-JarFile 'core\x.Engine.jar')
        )

        @($index.OwnersByClass['x.Engine']) | Should -Be @('core')
        $index.ShadowedClassCount | Should -Be 1
        $index.SharedClassCount | Should -Be 0
    }

    It 'names an owner once when two of its jars hold the same class' {

        $index = New-ClassOwnerIndex -ClassNames @('x.Dup') -Sources @(
            New-Source -Owner 'alpha' -Jars @((New-JarFile 'one\x.Dup.jar'), (New-JarFile 'two\x.Dup.jar'))
        )

        @($index.OwnersByClass['x.Dup']) | Should -Be @('alpha')
        $index.SharedClassCount | Should -Be 0
    }

    It 'skips a jar that is not on disk, and one no zip reader opens, returning each with its reason' {

        $missing = Join-Path $TestDrive 'gone.jar'
        $broken = New-JarFile 'broken.jar'

        $index = New-ClassOwnerIndex -ClassNames @('x.Kept') -Sources @(
            New-Source -Owner 'alpha' -Jars @($missing, $broken, (New-JarFile 'kept\x.Kept.jar'))
        )

        $index.SkippedJars.Path | Should -Be @($missing, $broken)
        $index.SkippedJars.Reason | Should -Be @('not on disk', 'End of Central Directory record could not be found.')
        $index.JarCount | Should -Be 1
        @($index.OwnersByClass['x.Kept']) | Should -Be @('alpha')
    }

    It 'compares class names by case, as Java does' {

        $index = New-ClassOwnerIndex -ClassNames @('x.name') -Sources @(
            New-Source -Owner 'alpha' -Jars @(New-JarFile 'case\x.Name.jar')
        )

        $index.OwnersByClass.Count | Should -Be 0
    }
}
