BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index\Get-CoreOwnerName.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Format-ObjectChainLine.ps1"

    function script:New-Link {
        # One class of an object chain, as Add-ObjectChainOwner leaves it.
        param(
            [int] $Depth,
            [string] $Class,
            [string[]] $Owners = @()
        )

        return [pscustomobject]@{ Depth = $Depth; Class = $Class; Owners = $Owners }
    }
}

Describe 'Format-ObjectChainLine' {

    BeforeEach {
        Mock Get-CoreOwnerName { 'core' }
    }

    It 'names the nearest mod class, with every owner, and counts the mod classes' {

        $load = [pscustomobject]@{
            SaveFolder  = 'save_A'
            ObjectChain = @(
                (New-Link -Depth 0 -Class 'a.Spec' -Owners 'core')
                (New-Link -Depth 1 -Class 'java.util.ArrayList')
                (New-Link -Depth 2 -Class 'm.Market' -Owners 'alpha', 'beta')
                (New-Link -Depth 3 -Class 'm.Lord' -Owners 'gamma')
            )
        }

        Format-ObjectChainLine -Load $load |
            Should -Be 'save_A: object chain length 4, mod classes 2, nearest m.Market (alpha, beta) at depth 2'
    }

    It 'counts a core class a mod also bundles as the game''s' {

        $load = [pscustomobject]@{
            SaveFolder  = 'save_A'
            ObjectChain = @(
                (New-Link -Depth 0 -Class 'a.Spec' -Owners 'core', 'alpha')
                (New-Link -Depth 1 -Class 'java.util.ArrayList')
            )
        }

        Format-ObjectChainLine -Load $load | Should -Be 'save_A: object chain length 2, mod classes 0'
    }
}
