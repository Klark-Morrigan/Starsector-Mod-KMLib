BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\install\Get-CoreOwnerName.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Select-ModChainLink.ps1"
}

Describe 'Select-ModChainLink' {

    BeforeEach {
        Mock Get-CoreOwnerName { 'core' }
    }

    It 'keeps the links a mod owns, in order, dropping core-owned and unowned classes' {

        # ARRANGE
        $chain = @(
            [pscustomobject]@{ Depth = 0; Class = 'a.Spec'; Owners = @('core') }
            [pscustomobject]@{ Depth = 1; Class = 'java.util.ArrayList'; Owners = @() }
            [pscustomobject]@{ Depth = 2; Class = 'm.Market'; Owners = @('alpha', 'beta') }
            [pscustomobject]@{ Depth = 3; Class = 'm.Lord'; Owners = @('gamma') }
        )

        $expectedClasses = @('m.Market', 'm.Lord')

        # ACT
        $actualLinks = Select-ModChainLink -ObjectChain $chain

        # ASSERT
        $actualLinks.Class | Should -Be $expectedClasses
    }

    It 'returns an empty list for a chain with no mod class' {

        # ARRANGE
        $coreLink = [pscustomobject]@{ Depth = 0; Class = 'a.Spec'; Owners = @('core') }

        $expectedCount = 0

        # ACT
        $actualLinks = Select-ModChainLink -ObjectChain @($coreLink)

        # ASSERT
        $null -eq $actualLinks | Should -BeFalse
        $actualLinks.Count | Should -Be $expectedCount
    }
}
