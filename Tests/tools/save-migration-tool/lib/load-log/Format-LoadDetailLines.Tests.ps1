BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$libRoot\Format-AttemptLines.ps1"
    . "$libRoot\Format-CauseLines.ps1"
    . "$libRoot\Format-LoadReadingLines.ps1"
    . "$libRoot\Format-ModClassLines.ps1"
    . "$libRoot\Format-LoadDetailLines.ps1"
}

Describe 'Format-LoadDetailLines' {

    BeforeEach {

        Mock Format-CauseLines { @('<type>', '<cause>') }
        Mock Format-LoadReadingLines { @('<missing>', '<chain counts>', '  <chain nearest>') }
        Mock Format-AttemptLines { @('<attempts>', '  <attempt>') }
        Mock Format-ModClassLines { @('<mod classes>', '  <mod class>') }
    }

    It 'indents each part under the load: the cause, its readings, the attempts and the mod classes' {

        # ARRANGE
        $load = [pscustomobject]@{
            CauseType   = 'T'
            Cause       = 'gone'
            ObjectChain = @([pscustomobject]@{ Depth = 0 })
            Attempts    = @('first', 'second')
        }

        $expectedLines = @(
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

        # ACT
        $actualLines = Format-LoadDetailLines -Load $load

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'leaves out the cause of a load that has none' {

        # ARRANGE
        Mock Format-LoadReadingLines { }
        Mock Format-AttemptLines { }
        Mock Format-ModClassLines { }

        $load = [pscustomobject]@{ CauseType = $null; ObjectChain = @(); Attempts = @('only') }
        $expectedCount = 0

        # ACT
        $actualLines = @(Format-LoadDetailLines -Load $load)

        # ASSERT
        $actualLines.Count | Should -Be $expectedCount
        Should -Invoke Format-CauseLines -Times 0
    }
}
