BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Add-ObjectChainOwner.ps1"
}

Describe 'Add-ObjectChainOwner' {

    BeforeEach {

        $script:OwnersByClass = @{
            'a.Mod'    = [System.Collections.Generic.List[string]]@('alpha', 'beta')
            'a.Engine' = [System.Collections.Generic.List[string]]@('vanilla')
        }
    }

    It 'adds each class in each attempt''s chain its owners' {

        # ARRANGE
        $load = [pscustomobject]@{
            Attempts = @(
                [pscustomobject]@{ ObjectChain = @(
                    [pscustomobject]@{ Depth = 0; Class = 'a.Mod' }
                    [pscustomobject]@{ Depth = 1; Class = 'a.Engine' }
                ) }
                [pscustomobject]@{ ObjectChain = @([pscustomobject]@{ Depth = 0; Class = 'a.Engine' }) }
            )
        }

        $expectedFirstChainClasses = @('a.Mod', 'a.Engine')
        $expectedFirstOwners = @('alpha', 'beta')
        $expectedSecondOwners = @('vanilla')

        # ACT
        $actualLoad = Add-ObjectChainOwner -Load $load -OwnersByClass $script:OwnersByClass

        # ASSERT
        $actualLoad.Attempts[0].ObjectChain.Class | Should -Be $expectedFirstChainClasses
        $actualLoad.Attempts[0].ObjectChain[0].Owners | Should -Be $expectedFirstOwners
        , $actualLoad.Attempts[1].ObjectChain[0].Owners | Should -BeOfType [object[]]
        $actualLoad.Attempts[1].ObjectChain[0].Owners | Should -Be $expectedSecondOwners
    }

    It 'gives a class no indexed JAR holds no owners' {

        # ARRANGE
        $link = [pscustomobject]@{ Depth = 2; Class = 'java.util.ArrayList' }
        $load = [pscustomobject]@{ Attempts = @([pscustomobject]@{ ObjectChain = @($link) }) }

        $expectedDepth = 2
        $expectedOwnerCount = 0

        # ACT
        $actualLoad = Add-ObjectChainOwner -Load $load -OwnersByClass $script:OwnersByClass

        # ASSERT
        $actualLink = $actualLoad.Attempts[0].ObjectChain[0]
        $actualLink.Depth | Should -Be $expectedDepth
        , $actualLink.Owners | Should -BeOfType [object[]]
        $actualLink.Owners.Count | Should -Be $expectedOwnerCount
    }
}
