BeforeAll {

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index"

    . "$libRoot\Get-CoreOwnerName.ps1"
    . "$libRoot\New-ClassOwnerIndex.ps1"
    . "$libRoot\Read-CoreClasspath.ps1"
    . "$libRoot\Resolve-ClassOwner.ps1"
}

Describe 'Resolve-ClassOwner' {

    BeforeEach {

        Mock Get-CoreOwnerName { 'core' }
        Mock Read-CoreClasspath { 'C:\Games\starsector-core\a.jar', 'C:\Games\starsector-core\b.jar' }
        Mock New-ClassOwnerIndex { [pscustomobject]@{ Sources = $Sources; ClassNames = $ClassNames } }
    }

    It 'indexes the core classpath first, then each roster mod''s jars, for the given classes' {

        $roster = @(
            [pscustomobject]@{ Id = 'alpha'; Jars = @('C:\Games\mods\alpha\alpha.jar') }
            [pscustomobject]@{ Id = 'beta'; Jars = @() }
        )

        $resolved = Resolve-ClassOwner -StarsectorRoot 'C:\Games' -Roster $roster -ClassNames @('x.A')

        $resolved.Sources.Owner | Should -Be @('core', 'alpha', 'beta')
        $resolved.Sources[0].Jars | Should -Be @('C:\Games\starsector-core\a.jar', 'C:\Games\starsector-core\b.jar')
        $resolved.Sources[1].Jars | Should -Be @('C:\Games\mods\alpha\alpha.jar')
        $resolved.ClassNames | Should -Be @('x.A')
        $resolved.CoreJarCount | Should -Be 2

        Should -Invoke Read-CoreClasspath -Times 1 -Exactly -ParameterFilter { $StarsectorRoot -eq 'C:\Games' }
    }

    It 'counts no core jars when vmparams names no classpath' {

        Mock Read-CoreClasspath { }

        (Resolve-ClassOwner -StarsectorRoot 'C:\Games' -Roster @() -ClassNames @('x.A')).CoreJarCount | Should -Be 0
    }
}
