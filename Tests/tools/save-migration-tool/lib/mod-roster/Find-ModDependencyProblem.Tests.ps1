BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\mod-info\Find-VersionProblem.ps1"
    . "$libRoot\mod-info\Test-JsonProperty.ps1"
    . "$libRoot\mod-roster\Find-ModDependencyProblem.ps1"
}

Describe 'Find-ModDependencyProblem' {

    BeforeEach {

        # The parser hands back ordered hashtables, so the fake looks the key up in one.
        Mock Test-JsonProperty { $Object -is [System.Collections.IDictionary] -and $Object.Contains($Name) }
        Mock Find-VersionProblem { $null }
    }

    It 'finds no problem in a mod_info.json naming no dependencies' {

        # ARRANGE
        $info = [ordered]@{ id = 'a' }
        $expectedProblem = $null

        # ACT
        $actualProblem = Find-ModDependencyProblem -Info $info

        # ASSERT
        $actualProblem | Should -Be $expectedProblem
    }

    # A list of one item is still a list, though PowerShell unrolls one returned from a function.
    It 'finds no problem in <Description>' -ForEach @(
        @{ Description = 'an empty list'; Dependencies = @() }
        @{ Description = 'one dependency with an ID'; Dependencies = @([ordered]@{ id = 'lazylib' }) }
    ) {

        # ARRANGE
        $info = [ordered]@{ dependencies = $Dependencies }
        $expectedProblem = $null

        # ACT
        $actualProblem = Find-ModDependencyProblem -Info $info

        # ASSERT
        $actualProblem | Should -Be $expectedProblem
    }

    # The game reads the list with getJSONArray, each item with getJSONObject, and each item's ID with getString,
    # and each throws on anything else.
    It 'names <Description>' -ForEach @(
        @{ Description = 'dependencies that are not a list'; Dependencies = 'lazylib'
            Problem = '"dependencies" is not a list' }
        @{ Description = 'dependencies written as null'; Dependencies = $null
            Problem = '"dependencies" is not a list' }
        @{ Description = 'a dependency that is not an object'; Dependencies = @([ordered]@{ id = 'a' }, 'b')
            Problem = 'dependency 2 is not an object' }
        @{ Description = 'a dependency with no ID'; Dependencies = @([ordered]@{ name = 'LazyLib' })
            Problem = 'no "id" in dependency 1' }
    ) {

        # ARRANGE
        $info = [ordered]@{ dependencies = $Dependencies }
        $expectedProblem = $Problem

        # ACT
        $actualProblem = Find-ModDependencyProblem -Info $info

        # ASSERT
        $actualProblem | Should -Be $expectedProblem
    }

    It 'names a dependency whose version the game cannot read' {

        # ARRANGE
        Mock Find-VersionProblem { 'no "major" in "version"' }

        $info = [ordered]@{ dependencies = @([ordered]@{ id = 'lazylib'; version = @{} }) }
        $expectedProblem = 'no "major" in "version" of dependency 1'

        # ACT
        $actualProblem = Find-ModDependencyProblem -Info $info

        # ASSERT
        $actualProblem | Should -Be $expectedProblem
    }
}
