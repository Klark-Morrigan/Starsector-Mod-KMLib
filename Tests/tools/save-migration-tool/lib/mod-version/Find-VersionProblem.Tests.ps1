BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\data-file\Get-JsonPropertyValue.ps1"
    . "$libRoot\data-file\Test-JsonProperty.ps1"
    . "$libRoot\mod-version\Find-VersionProblem.ps1"
    . "$libRoot\mod-version\Test-VersionObjectForm.ps1"
}

Describe 'Find-VersionProblem' {

    BeforeEach {
        Mock Get-JsonPropertyValue { 'the version' }
    }

    # The game's VersionInfo reads a version object's "major" with getString, which throws on a missing key.
    It 'names a version object with no "major"' {

        # ARRANGE
        Mock Test-VersionObjectForm { $true }
        Mock Test-JsonProperty { $false }

        $expectedProblem = 'no "major" in "gameVersion"'

        # ACT
        $actualProblem = Find-VersionProblem -Owner @{} -Name 'gameVersion'

        # ASSERT
        $actualProblem | Should -Be $expectedProblem
    }

    # The game reads a version string, or no version at all, as text, whatever it holds.
    It 'finds no problem in <Description>' -ForEach @(
        @{ Description = 'a version object with a "major"'; IsObjectForm = $true; HasMajor = $true }
        @{ Description = 'a version in any other form'; IsObjectForm = $false; HasMajor = $false }
    ) {

        # ARRANGE
        Mock Test-VersionObjectForm { $IsObjectForm }
        Mock Test-JsonProperty { $HasMajor }

        $expectedProblem = $null

        # ACT
        $actualProblem = Find-VersionProblem -Owner @{} -Name 'version'

        # ASSERT
        $actualProblem | Should -Be $expectedProblem
    }
}
