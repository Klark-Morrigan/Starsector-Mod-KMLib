BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$libRoot\Format-AttemptLines.ps1"
    . "$libRoot\Format-CauseLines.ps1"
    . "$libRoot\Format-MissingContentLine.ps1"
    . "$libRoot\Format-ModClassLines.ps1"
    . "$libRoot\Format-ObjectChainLines.ps1"
    . "$libRoot\Format-LoadDetailLines.ps1"
}

Describe 'Format-LoadDetailLines' {

    BeforeEach {

        Mock Format-CauseLines { @('<type>', '<cause>') }
        Mock Format-MissingContentLine { '<missing>' }
        Mock Format-ObjectChainLines { @('<chain counts>', '  <chain nearest>') }
        Mock Format-AttemptLines { @('<attempts>', '  <attempt>') }
        Mock Format-ModClassLines { @('<mod classes>', '  <mod class>') }
    }

    It 'indents each part under the load: the cause, its readings, the attempts and the mod classes' {

        $load = [pscustomobject]@{
            CauseType   = 'T'
            Cause       = 'gone'
            ObjectChain = @([pscustomobject]@{ Depth = 0 })
            Attempts    = @('first', 'second')
        }

        Format-LoadDetailLines -Load $load | Should -Be @(
            '  Cause:'
            '    <type>'
            '    <cause>'
            '  <missing>'
            '  <chain counts>'
            '    <chain nearest>'
            '  <attempts>'
            '    <attempt>'
            '  <mod classes>'
            '    <mod class>'
        )
    }

    It 'leaves out the cause and chain of a load that has none' {

        Mock Format-AttemptLines { }
        Mock Format-ModClassLines { }

        $load = [pscustomobject]@{ CauseType = $null; ObjectChain = @(); Attempts = @('only') }

        @(Format-LoadDetailLines -Load $load).Count | Should -Be 0

        Should -Invoke Format-MissingContentLine -Times 0
        Should -Invoke Format-ObjectChainLines -Times 0
    }
}
