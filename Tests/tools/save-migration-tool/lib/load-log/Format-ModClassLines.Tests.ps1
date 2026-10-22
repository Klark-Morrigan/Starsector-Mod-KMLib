BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\load-log\Format-OwnerList.ps1"
    . "$libRoot\load-log\Select-ModChainLink.ps1"
    . "$libRoot\load-log\Format-ModClassLines.ps1"
}

Describe 'Format-ModClassLines' {

    BeforeEach {

        Mock Select-ModChainLink { ,@($ObjectChain | Where-Object { $_.Class -like 'm.*' }) }
        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ConsoleNumber { "<Number>$Number" }
        Mock Format-OwnerList { "<Owners>$($Owners -join '+')" }
    }

    It 'lists each mod class, innermost first, with its depth and owners, under a heading' {

        $chain = @(
            [pscustomobject]@{ Depth = 0; Class = 'a.Spec'; Owners = @('vanilla') }
            [pscustomobject]@{ Depth = 3; Class = 'm.Market'; Owners = @('alpha', 'beta') }
            [pscustomobject]@{ Depth = 5; Class = 'm.Lord'; Owners = @('gamma') }
        )

        Format-ModClassLines -ObjectChain $chain | Should -Be @(
            'Mod classes in the object chain, innermost first:'
            '  depth <Number>3: <Code>m.Market (<Owners>alpha+beta)'
            '  depth <Number>5: <Code>m.Lord (<Owners>gamma)'
        )
    }

    It 'writes nothing for a chain holding no mod class' {

        $chain = @([pscustomobject]@{ Depth = 0; Class = 'a.Spec'; Owners = @('vanilla') })

        @(Format-ModClassLines -ObjectChain $chain).Count | Should -Be 0
    }
}
