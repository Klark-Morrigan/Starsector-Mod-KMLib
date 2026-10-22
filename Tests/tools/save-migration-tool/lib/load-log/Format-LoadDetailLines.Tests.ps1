BeforeAll {

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\load-log\Format-LoadOutcome.ps1"
    . "$libRoot\load-log\Format-MissingContentLine.ps1"
    . "$libRoot\load-log\Format-ObjectChainLines.ps1"
    . "$libRoot\load-log\Select-ModChainLink.ps1"
    . "$libRoot\load-log\Format-LoadDetailLines.ps1"
}

Describe 'Format-LoadDetailLines' {

    BeforeEach {

        # Marks each painted value with its role, so a case pins the colour of every value it names.
        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ConsoleNumber { "<Number>$Number" }
        Mock Format-LoadOutcome { "<$Outcome>" }
        Mock Format-MissingContentLine { 'missing line' }
        Mock Format-ObjectChainLines { @('chain counts,', '  chain nearest') }
        Mock Select-ModChainLink { ,@($ObjectChain | Where-Object { $_.Class -like 'm.*' }) }
    }

    It 'nests the cause, its readings, each retry in order and each mod class, one value per line' {

        $load = [pscustomobject]@{
            CauseType   = 'java.lang.RuntimeException'
            Cause       = 'Ship hull spec [h_default_D] not found!'
            ObjectChain = @(
                [pscustomobject]@{ Depth = 0; Class = 'a.Spec'; Owners = @('vanilla') }
                [pscustomobject]@{ Depth = 3; Class = 'm.Market'; Owners = @('alpha', 'beta') }
            )
            Attempts    = @(
                [pscustomobject]@{
                    Source = 'campaign'; Outcome = 'Failed'; LogFile = 'C:\logs\starsector.log.1'; LogLine = 12
                    CauseType = 'java.lang.RuntimeException'; Cause = 'Ship hull spec [h_default_D] not found!'
                }
                [pscustomobject]@{
                    Source = 'campaign.bak'; Outcome = 'Loaded'; LogFile = 'C:\logs\starsector.log'; LogLine = 3
                    CauseType = $null; Cause = $null
                }
            )
        }

        Format-LoadDetailLines -Load $load | Should -Be @(
            '  Cause:'
            '    <Code>java.lang.RuntimeException'
            '    <Error>Ship hull spec [h_default_D] not found!'
            '  missing line'
            '  chain counts,'
            '    chain nearest'
            '  Attempts, in order:'
            '    <Path>campaign:'
            '      <Failed> at <Path>starsector.log.1:<Number>12'
            '      <Code>java.lang.RuntimeException'
            '      <Error>Ship hull spec [h_default_D] not found!'
            '    <Path>campaign.bak:'
            '      <Loaded> at <Path>starsector.log:<Number>3'
            '  Mod classes in the object chain, innermost first:'
            '    depth <Number>3: <Code>m.Market (<Mod>alpha, <Mod>beta)'
        )
    }

    It 'writes nothing for a load tried once that loaded' {

        $load = [pscustomobject]@{
            CauseType   = $null
            ObjectChain = @()
            Attempts    = @([pscustomobject]@{ Source = 'campaign'; Outcome = 'Loaded' })
        }

        @(Format-LoadDetailLines -Load $load).Count | Should -Be 0
    }
}
