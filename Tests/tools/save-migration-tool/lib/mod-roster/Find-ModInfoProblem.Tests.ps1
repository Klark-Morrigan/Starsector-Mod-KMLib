BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\mod-info\Find-VersionProblem.ps1"
    . "$libRoot\mod-info\Test-JsonProperty.ps1"
    . "$libRoot\mod-roster\Find-ModDependencyProblem.ps1"
    . "$libRoot\mod-roster\Find-ModInfoProblem.ps1"
}

Describe 'Find-ModInfoProblem' {

    BeforeEach {

        # The parser hands back ordered hashtables, so the fake looks the key up in one.
        Mock Test-JsonProperty { $Object -is [System.Collections.IDictionary] -and $Object.Contains($Name) }
        Mock Find-VersionProblem { $null }
        Mock Find-ModDependencyProblem { $null }
    }

    It 'finds no problem in a mod_info.json the game reads' {

        # ARRANGE
        $info = [ordered]@{ id = 'a'; name = 'A'; description = '' }
        $expectedProblem = $null

        # ACT
        $actualProblem = Find-ModInfoProblem -Info $info

        # ASSERT
        $actualProblem | Should -Be $expectedProblem
    }

    # The game reads description, then id, then name, so the first missing in that order is the one it throws on.
    It 'names the first required field missing, <Description>' -ForEach @(
        @{ Description = 'description before ID'; Info = [ordered]@{ name = 'A' }; Problem = 'no "description" field' }
        @{ Description = 'ID before name'; Info = [ordered]@{ description = '' }; Problem = 'no "id" field' }
        @{ Description = 'name last'; Info = [ordered]@{ description = ''; id = 'a' }; Problem = 'no "name" field' }
    ) {

        # ARRANGE
        $expectedProblem = $Problem

        # ACT
        $actualProblem = Find-ModInfoProblem -Info $Info

        # ASSERT
        $actualProblem | Should -Be $expectedProblem
    }

    It 'names a version the game cannot read' {

        # ARRANGE
        Mock Find-VersionProblem { if ($Name -eq 'gameVersion') { 'no "major" in "gameVersion"' } }

        $info = [ordered]@{ id = 'a'; name = 'A'; description = '' }
        $expectedProblem = 'no "major" in "gameVersion"'

        # ACT
        $actualProblem = Find-ModInfoProblem -Info $info

        # ASSERT
        $actualProblem | Should -Be $expectedProblem
    }

    It 'names a dependency the game cannot read' {

        # ARRANGE
        Mock Find-ModDependencyProblem { 'no "id" in dependency 1' }

        $info = [ordered]@{ id = 'a'; name = 'A'; description = '' }
        $expectedProblem = 'no "id" in dependency 1'

        # ACT
        $actualProblem = Find-ModInfoProblem -Info $info

        # ASSERT
        $actualProblem | Should -Be $expectedProblem
    }
}
