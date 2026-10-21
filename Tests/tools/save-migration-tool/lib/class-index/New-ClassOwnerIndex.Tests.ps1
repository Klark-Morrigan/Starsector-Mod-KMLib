BeforeAll {

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index"

    . "$libRoot\New-ClassOwnerIndex.ps1"
    . "$libRoot\Read-JarClassNames.ps1"

    function script:New-JarFile {
        # Writes an empty file under TestDrive for a jar path to exist; the jar reader is mocked.
        param([string] $Name)

        $path = Join-Path $TestDrive $Name

        New-Item -ItemType File -Path $path -Force | Out-Null

        return $path
    }
}

Describe 'New-ClassOwnerIndex' {

    BeforeEach {

        # Each jar's file name lists its classes: a.B+c.D.jar holds a.B and c.D.
        Mock Read-JarClassNames { [System.IO.Path]::GetFileNameWithoutExtension($Path) -split '\+' }
    }

    It 'names the owner of each class in every jar of a source' {

        $index = New-ClassOwnerIndex -Sources @(
            [pscustomobject]@{ Owner = 'alpha'; Jars = @((New-JarFile 'a.One.jar'), (New-JarFile 'a.Two.jar')) }
        )

        @($index.OwnersByClass['a.One']) | Should -Be @('alpha')
        @($index.OwnersByClass['a.Two']) | Should -Be @('alpha')
        $index.JarCount | Should -Be 2
    }

    It 'keeps every owner of a class two sources hold, in source order, and counts it as shared' {

        $index = New-ClassOwnerIndex -Sources @(
            [pscustomobject]@{ Owner = 'vanilla'; Jars = @(New-JarFile 'core\x.Shared+x.Core.jar') }
            [pscustomobject]@{ Owner = 'alpha'; Jars = @(New-JarFile 'alpha\x.Shared.jar') }
        )

        @($index.OwnersByClass['x.Shared']) | Should -Be @('vanilla', 'alpha')
        $index.SharedClassCount | Should -Be 1
    }

    It 'names an owner once when two of its jars hold the same class' {

        $index = New-ClassOwnerIndex -Sources @(
            [pscustomobject]@{ Owner = 'alpha'; Jars = @((New-JarFile 'one\x.Dup.jar'), (New-JarFile 'two\x.Dup.jar')) }
        )

        @($index.OwnersByClass['x.Dup']) | Should -Be @('alpha')
        $index.SharedClassCount | Should -Be 0
    }

    It 'skips a jar that is not on disk and returns it' {

        $missing = Join-Path $TestDrive 'gone.jar'

        $index = New-ClassOwnerIndex -Sources @([pscustomobject]@{ Owner = 'alpha'; Jars = @($missing) })

        $index.MissingJars | Should -Be @($missing)
        $index.JarCount | Should -Be 0
        $index.OwnersByClass.Count | Should -Be 0

        Should -Invoke Read-JarClassNames -Times 0 -Exactly
    }

    It 'compares class names by case, as Java does' {

        $index = New-ClassOwnerIndex -Sources @(
            [pscustomobject]@{ Owner = 'alpha'; Jars = @(New-JarFile 'case\x.Name.jar') }
        )

        $index.OwnersByClass.ContainsKey('x.name') | Should -BeFalse
    }
}
