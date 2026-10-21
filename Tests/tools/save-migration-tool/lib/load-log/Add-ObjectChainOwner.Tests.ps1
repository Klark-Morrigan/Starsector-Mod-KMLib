BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Add-ObjectChainOwner.ps1"
}

Describe 'Add-ObjectChainOwner' {

    BeforeEach {

        $script:OwnersByClass = @{
            'a.Mod'    = [System.Collections.Generic.List[string]]@('alpha', 'beta')
            'a.Engine' = [System.Collections.Generic.List[string]]@('vanilla')
        }
    }

    It 'adds each class in each attempt''s chain its owners, and the load its first attempt''s chain' {

        $load = [pscustomobject]@{
            Attempts = @(
                [pscustomobject]@{ ObjectChain = @(
                    [pscustomobject]@{ Depth = 0; Class = 'a.Mod' }
                    [pscustomobject]@{ Depth = 1; Class = 'a.Engine' }
                ) }
                [pscustomobject]@{ ObjectChain = @([pscustomobject]@{ Depth = 0; Class = 'a.Engine' }) }
            )
        }

        $result = Add-ObjectChainOwner -Load $load -OwnersByClass $script:OwnersByClass

        $result.ObjectChain.Class | Should -Be @('a.Mod', 'a.Engine')
        $result.ObjectChain[0].Owners | Should -Be @('alpha', 'beta')
        , $result.Attempts[1].ObjectChain[0].Owners | Should -BeOfType [object[]]
        $result.Attempts[1].ObjectChain[0].Owners | Should -Be @('vanilla')
    }

    It 'gives a class no indexed jar holds no owners' {

        $link = [pscustomobject]@{ Depth = 2; Class = 'java.util.ArrayList' }
        $load = [pscustomobject]@{ Attempts = @([pscustomobject]@{ ObjectChain = @($link) }) }

        $owned = (Add-ObjectChainOwner -Load $load -OwnersByClass $script:OwnersByClass).ObjectChain[0]

        $owned.Depth | Should -Be 2
        , $owned.Owners | Should -BeOfType [object[]]
        $owned.Owners.Count | Should -Be 0
    }

    It 'gives the load an empty chain when the first attempt names none' {

        $load = [pscustomobject]@{ Attempts = @([pscustomobject]@{ ObjectChain = @() }) }

        @((Add-ObjectChainOwner -Load $load -OwnersByClass @{}).ObjectChain).Count | Should -Be 0
    }
}
