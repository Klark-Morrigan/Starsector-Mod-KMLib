BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index"

    . "$libRoot\Get-CoreOwnerName.ps1"
    . "$libRoot\New-ClassOwnerIndex.ps1"
    . "$libRoot\New-InstallClassOwnerIndex.ps1"
    . "$libRoot\Read-CoreClasspath.ps1"
}

Describe 'New-InstallClassOwnerIndex' {

    BeforeEach {

        Mock Get-CoreOwnerName { 'core' }
        Mock Read-CoreClasspath {
            [pscustomobject]@{
                Jars    = @('C:\Games\starsector-core\a.jar', 'C:\Games\starsector-core\b.jar')
                Problem = $null
            }
        }
        Mock New-ClassOwnerIndex { [pscustomobject]@{ Sources = $Sources; ClassNames = $ClassNames } }
    }

    # The game loads its own classes before any mod's, so the core comes first.
    It 'indexes the core classpath first, then each roster mod''s JARs, for the given classes' {

        # ARRANGE
        $roster = @(
            [pscustomobject]@{ Id = 'alpha'; Jars = @('C:\Games\mods\alpha\alpha.jar') }
            [pscustomobject]@{ Id = 'beta'; Jars = @() }
        )

        $expectedOwners = @('core', 'alpha', 'beta')
        $expectedCoreJars = @('C:\Games\starsector-core\a.jar', 'C:\Games\starsector-core\b.jar')
        $expectedAlphaJars = @('C:\Games\mods\alpha\alpha.jar')
        $expectedClassNames = @('x.A')
        $expectedEntryCount = 2

        # ACT
        $actualIndex = New-InstallClassOwnerIndex -StarsectorRoot 'C:\Games' -Roster $roster -ClassNames @('x.A')

        # ASSERT
        $actualIndex.Sources.Owner | Should -Be $expectedOwners
        $actualIndex.Sources[0].Jars | Should -Be $expectedCoreJars
        $actualIndex.Sources[1].Jars | Should -Be $expectedAlphaJars
        $actualIndex.ClassNames | Should -Be $expectedClassNames
        $actualIndex.CoreClasspathEntryCount | Should -Be $expectedEntryCount
        $actualIndex.CoreClasspathProblem | Should -BeNullOrEmpty

        Should -Invoke Read-CoreClasspath -Times 1 -Exactly -ParameterFilter { $StarsectorRoot -eq 'C:\Games' }
    }

    # The caller warns with the reason, so a missing vmparams does not read as an install without core classes.
    It 'counts no core classpath entries, passing on why, when the classpath could not be read' {

        # ARRANGE
        Mock Read-CoreClasspath { [pscustomobject]@{ Jars = @(); Problem = 'C:\Games\vmparams not found' } }

        $expectedEntryCount = 0
        $expectedProblem = 'C:\Games\vmparams not found'

        # ACT
        $actualIndex = New-InstallClassOwnerIndex -StarsectorRoot 'C:\Games' -Roster @() -ClassNames @('x.A')

        # ASSERT
        $actualIndex.CoreClasspathEntryCount | Should -Be $expectedEntryCount
        $actualIndex.CoreClasspathProblem | Should -Be $expectedProblem
    }
}
