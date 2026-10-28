BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleList.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\load-log\Select-ModChainLink.ps1"
    . "$libRoot\load-log\Format-ObjectChainLines.ps1"

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

Describe 'Format-ObjectChainLines' {

    BeforeEach {

        # Mod classes are the ones named m.*, so each case states which links a mod owns by their names.
        Mock Select-ModChainLink { ,@($ObjectChain | Where-Object { $_.Class -like 'm.*' }) }

        # Marks each painted value with its role, so a case pins the colour of every value it names.
        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ConsoleNumber { "<Number>$Number" }
        Mock Format-ConsoleList { "<$Role>$($Values -join '+')" }
    }

    It 'counts the chain, then names the nearest mod class with its owners on an indented line' {

        # ARRANGE
        $load = [pscustomobject]@{
            SaveFolder  = 'save_A'
            ObjectChain = @(
                (New-Link -Depth 0 -Class 'a.Spec' -Owners 'core')
                (New-Link -Depth 1 -Class 'java.util.ArrayList')
                (New-Link -Depth 2 -Class 'm.Market' -Owners 'alpha', 'beta')
                (New-Link -Depth 3 -Class 'm.Lord' -Owners 'gamma')
            )
        }

        $expectedLines = @(
            'object chain length <Number>4, mod classes <Number>2,'
            '  nearest <Code>m.Market (<Mod>alpha+beta) at depth <Number>2'
        )

        # ACT
        $actualLines = Format-ObjectChainLines -Load $load

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'counts no mod class in a chain of core and unowned classes, in one line' {

        # ARRANGE
        $load = [pscustomobject]@{
            SaveFolder  = 'save_A'
            ObjectChain = @(
                (New-Link -Depth 0 -Class 'a.Spec' -Owners 'core')
                (New-Link -Depth 1 -Class 'java.util.ArrayList')
            )
        }

        $expectedLines = @('object chain length <Number>2, mod classes <Number>0')

        # ACT
        $actualLines = Format-ObjectChainLines -Load $load

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }
}
