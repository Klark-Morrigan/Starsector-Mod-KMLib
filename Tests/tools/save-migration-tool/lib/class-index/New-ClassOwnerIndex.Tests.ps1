BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\class-index\New-ClassOwnerIndex.ps1"
    . "$libRoot\class-index\Select-JarClassNames.ps1"
    . "$libRoot\install\Get-CoreOwnerName.ps1"
    . "$libRoot\mod-roster\Get-ModIdComparer.ps1"

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
        Mock Get-ModIdComparer { [System.StringComparer]::Ordinal }

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

        # ARRANGE
        $sources = @(
            New-Source -Owner 'alpha' -Jars @(
                (New-TestFile -Name 'a.One+a.Unwanted.jar')
                (New-TestFile -Name 'a.Two.jar')
            )
        )
        $expectedOwners = @('alpha')
        $expectedModJarCount = 2

        # ACT
        $actualIndex = New-ClassOwnerIndex -ClassNames @('a.One', 'a.Two') -Sources $sources

        # ASSERT
        @($actualIndex.OwnersByClass['a.One']) | Should -Be $expectedOwners
        @($actualIndex.OwnersByClass['a.Two']) | Should -Be $expectedOwners
        $actualIndex.OwnersByClass.ContainsKey('a.Unwanted') | Should -BeFalse
        $actualIndex.ModJarCount | Should -Be $expectedModJarCount
        Should -Invoke Select-JarClassNames -ParameterFilter { "$ClassNames" -eq 'a.One a.Two' } -Times 2 -Exactly
    }

    # A caller reports the mods' JARs against the mods, and the core's against the classpath it listed.
    It 'counts the JARs read for the mods apart from those read for the core' {

        # ARRANGE
        $sources = @(
            New-Source -Owner 'core' -Jars @((New-TestFile -Name 'core\a.jar'), (New-TestFile -Name 'core\b.jar'))
            New-Source -Owner 'alpha' -Jars @(New-TestFile -Name 'alpha\c.jar')
        )
        $expectedCoreJarCount = 2
        $expectedModJarCount = 1

        # ACT
        $actualIndex = New-ClassOwnerIndex -ClassNames @('x.A') -Sources $sources

        # ASSERT
        $actualIndex.CoreJarCount | Should -Be $expectedCoreJarCount
        $actualIndex.ModJarCount | Should -Be $expectedModJarCount
    }

    # Owners match as mod IDs do, so a mod whose ID differs from the core's name only in case stays a mod.
    It 'tells the core owner apart by the mod ID comparer' {

        # ARRANGE
        $sources = @(New-Source -Owner 'Core' -Jars @(New-TestFile -Name 'Core\a.jar'))
        $expectedCoreJarCount = 0
        $expectedModJarCount = 1

        # ACT
        $actualIndex = New-ClassOwnerIndex -ClassNames @('x.A') -Sources $sources

        # ASSERT
        $actualIndex.CoreJarCount | Should -Be $expectedCoreJarCount
        $actualIndex.ModJarCount | Should -Be $expectedModJarCount
        Should -Invoke Get-ModIdComparer -Times 1 -Exactly
    }

    It 'keeps every mod owning a class two mods hold, in source order, and counts it as shared' {

        # ARRANGE
        $sources = @(
            New-Source -Owner 'alpha' -Jars @(New-TestFile -Name 'alpha\x.Shared.jar')
            New-Source -Owner 'beta' -Jars @(New-TestFile -Name 'beta\x.Shared.jar')
        )
        $expectedOwners = @('alpha', 'beta')
        $expectedSharedClassCount = 1
        $expectedShadowedClassCount = 0

        # ACT
        $actualIndex = New-ClassOwnerIndex -ClassNames @('x.Shared') -Sources $sources

        # ASSERT
        @($actualIndex.OwnersByClass['x.Shared']) | Should -Be $expectedOwners
        $actualIndex.SharedClassCount | Should -Be $expectedSharedClassCount
        $actualIndex.ShadowedClassCount | Should -Be $expectedShadowedClassCount
    }

    It 'keeps the core as the only owner of a class a mod also holds, whichever source comes first' {

        # ARRANGE
        $sources = @(
            New-Source -Owner 'alpha' -Jars @(New-TestFile -Name 'alpha\shadow\x.Engine.jar')
            New-Source -Owner 'core' -Jars @(New-TestFile -Name 'core\x.Engine.jar')
        )
        $expectedOwners = @('core')
        $expectedShadowedClassCount = 1
        $expectedSharedClassCount = 0

        # ACT
        $actualIndex = New-ClassOwnerIndex -ClassNames @('x.Engine') -Sources $sources

        # ASSERT
        @($actualIndex.OwnersByClass['x.Engine']) | Should -Be $expectedOwners
        $actualIndex.ShadowedClassCount | Should -Be $expectedShadowedClassCount
        $actualIndex.SharedClassCount | Should -Be $expectedSharedClassCount
    }

    It 'names an owner once when two of its JARs hold the same class' {

        # ARRANGE
        $sources = @(
            New-Source -Owner 'alpha' -Jars @(
                (New-TestFile -Name 'one\x.Dup.jar')
                (New-TestFile -Name 'two\x.Dup.jar')
            )
        )
        $expectedOwners = @('alpha')
        $expectedSharedClassCount = 0

        # ACT
        $actualIndex = New-ClassOwnerIndex -ClassNames @('x.Dup') -Sources $sources

        # ASSERT
        @($actualIndex.OwnersByClass['x.Dup']) | Should -Be $expectedOwners
        $actualIndex.SharedClassCount | Should -Be $expectedSharedClassCount
    }

    It 'skips a JAR that is not on disk, and one no zip reader opens, returning each with its reason' {

        # ARRANGE
        $missing = Join-Path $TestDrive 'gone.jar'
        $broken = New-TestFile -Name 'broken.jar'
        $sources = @(
            New-Source -Owner 'alpha' -Jars @($missing, $broken, (New-TestFile -Name 'kept\x.Kept.jar'))
        )
        $expectedSkippedPaths = @($missing, $broken)
        $expectedReasons = @('not on disk', 'End of Central Directory record could not be found.')
        $expectedModJarCount = 1
        $expectedOwners = @('alpha')

        # ACT
        $actualIndex = New-ClassOwnerIndex -ClassNames @('x.Kept') -Sources $sources

        # ASSERT
        $actualIndex.SkippedJars.Path | Should -Be $expectedSkippedPaths
        $actualIndex.SkippedJars.Reason | Should -Be $expectedReasons
        $actualIndex.ModJarCount | Should -Be $expectedModJarCount
        @($actualIndex.OwnersByClass['x.Kept']) | Should -Be $expectedOwners
    }
}
