BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index\Get-CoreOwnerName.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Select-ModChainLink.ps1"
}

Describe 'Select-ModChainLink' {

    BeforeEach {
        Mock Get-CoreOwnerName { 'core' }
    }

    It 'keeps the links a mod owns, in order, dropping core-owned and unowned classes' {

        $chain = @(
            [pscustomobject]@{ Depth = 0; Class = 'a.Spec'; Owners = @('core') }
            [pscustomobject]@{ Depth = 1; Class = 'java.util.ArrayList'; Owners = @() }
            [pscustomobject]@{ Depth = 2; Class = 'm.Market'; Owners = @('alpha', 'beta') }
            [pscustomobject]@{ Depth = 3; Class = 'm.Lord'; Owners = @('gamma') }
        )

        (Select-ModChainLink -ObjectChain $chain).Class | Should -Be @('m.Market', 'm.Lord')
    }

    It 'returns an empty list for a chain with no mod class' {

        $coreLink = [pscustomobject]@{ Depth = 0; Class = 'a.Spec'; Owners = @('core') }

        $links = Select-ModChainLink -ObjectChain @($coreLink)

        $null -eq $links | Should -BeFalse
        $links.Count | Should -Be 0
    }
}
