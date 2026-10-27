BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Add-ContentOwner.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Add-GeneratedContent.ps1"
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

        $rule = [pscustomobject]@{ Suffix = '_Hull'; IsSuffixedBaseSkipped = $false }

        $count = Add-GeneratedContent -OwnersById $script:Target -BaseIds @('alpha_hull', 'beta_hull') `
            -BaseOwners $script:BaseOwners -Rule $rule

        $script:Added | Should -Be @('alpha_hull_Hull=alpha', 'alpha_hull_Hull=vanilla', 'beta_hull_Hull=beta')
        $count | Should -Be 2

        Should -Invoke Add-ContentOwner -Times 3 -Exactly -ParameterFilter {
            [object]::ReferenceEquals($OwnersById, $script:Target)
        }
    }

    # The game makes no d-hull of a hull whose ID already names one.
    It 'skips a base whose ID already ends in the suffix, when the rule says so' {

        $rule = [pscustomobject]@{ Suffix = '_default_D'; IsSuffixedBaseSkipped = $true }

        $count = Add-GeneratedContent -OwnersById $script:Target -BaseIds @('beta_hull', 'already_default_D') `
            -BaseOwners $script:BaseOwners -Rule $rule

        $script:Added | Should -Be @('beta_hull_default_D=beta')
        $count | Should -Be 1
    }

    It 'builds from a suffixed base when the rule does not skip it' {

        $rule = [pscustomobject]@{ Suffix = '_default_D'; IsSuffixedBaseSkipped = $false }

        Add-GeneratedContent -OwnersById $script:Target -BaseIds @('already_default_D') `
            -BaseOwners $script:BaseOwners -Rule $rule | Out-Null

        $script:Added | Should -Be @('already_default_D_default_D=gamma')
    }
}
