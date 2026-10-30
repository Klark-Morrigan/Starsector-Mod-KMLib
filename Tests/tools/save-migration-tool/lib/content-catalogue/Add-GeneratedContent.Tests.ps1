BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\content-catalogue\owner-index\Add-ContentOwner.ps1"
    . "$libRoot\content-catalogue\Add-GeneratedContent.ps1"
}

Describe 'Add-GeneratedContent' {

    BeforeEach {

        $script:Added = [System.Collections.Generic.List[string]]::new()
        $script:Target = New-OwnerIndex
        $script:BaseOwners = New-OwnerIndex @{
            'alpha_hull'           = @('alpha', 'vanilla')
            'beta_hull'            = @('beta')
            'already_default_D'    = @('gamma')
        }

        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }
    }

    It 'adds each base ID plus the suffix, owned by each owner of its base, and counts the IDs built' {

        # ARRANGE
        $rule = [pscustomobject]@{ Suffix = '_Hull'; IsSuffixedBaseSkipped = $false }

        $expectedAdded = @('alpha_hull_Hull=alpha', 'alpha_hull_Hull=vanilla', 'beta_hull_Hull=beta')
        $expectedCount = 2
        $expectedAddCount = 3

        # ACT
        $actualCount = Add-GeneratedContent -OwnersById $script:Target -BaseIds @('alpha_hull', 'beta_hull') `
            -BaseOwners $script:BaseOwners -Rule $rule

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualCount | Should -Be $expectedCount

        Should -Invoke Add-ContentOwner -Times $expectedAddCount -Exactly -ParameterFilter {
            [object]::ReferenceEquals($OwnersById, $script:Target)
        }
    }

    # The game makes no d-hull of a hull whose ID already names one.
    It 'skips a base whose ID already ends in the suffix, when the rule says so' {

        # ARRANGE
        $rule = [pscustomobject]@{ Suffix = '_default_D'; IsSuffixedBaseSkipped = $true }

        $expectedAdded = @('beta_hull_default_D=beta')
        $expectedCount = 1

        # ACT
        $actualCount = Add-GeneratedContent -OwnersById $script:Target -BaseIds @('beta_hull', 'already_default_D') `
            -BaseOwners $script:BaseOwners -Rule $rule

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualCount | Should -Be $expectedCount
    }

    It 'builds from a suffixed base when the rule does not skip it' {

        # ARRANGE
        $rule = [pscustomobject]@{ Suffix = '_default_D'; IsSuffixedBaseSkipped = $false }

        $expectedAdded = @('already_default_D_default_D=gamma')

        # ACT
        Add-GeneratedContent -OwnersById $script:Target -BaseIds @('already_default_D') `
            -BaseOwners $script:BaseOwners -Rule $rule | Out-Null

        # ASSERT
        $script:Added | Should -Be $expectedAdded
    }
}
