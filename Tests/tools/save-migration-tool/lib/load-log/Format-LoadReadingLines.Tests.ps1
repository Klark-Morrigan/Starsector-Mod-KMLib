BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$libRoot\Format-MissingContentLine.ps1"
    . "$libRoot\Format-ObjectChainLines.ps1"
    . "$libRoot\Format-LoadReadingLines.ps1"
}

Describe 'Format-LoadReadingLines' {

    BeforeEach {

        Mock Format-MissingContentLine { '<missing>' }
        Mock Format-ObjectChainLines { @('<chain counts>', '  <chain nearest>') }
    }

    It 'writes the missing content, then the object chain' {

        # ARRANGE
        $load = [pscustomobject]@{ CauseType = 'T'; ObjectChain = @([pscustomobject]@{ Depth = 0 }) }
        $expectedLines = @('<missing>', '<chain counts>', '  <chain nearest>')

        # ACT
        $actualLines = Format-LoadReadingLines -Load $load

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'writes the missing content alone for a cause with no object chain' {

        # ARRANGE
        $load = [pscustomobject]@{ CauseType = 'T'; ObjectChain = @() }
        $expectedLines = @('<missing>')

        # ACT
        $actualLines = Format-LoadReadingLines -Load $load

        # ASSERT
        $actualLines | Should -Be $expectedLines
        Should -Invoke Format-ObjectChainLines -Times 0
    }

    # A load that worked has no cause, so it has no missing content to name.
    It 'writes nothing for a load with no cause and no object chain' {

        # ARRANGE
        $load = [pscustomobject]@{ CauseType = $null; ObjectChain = @() }
        $expectedCount = 0

        # ACT
        $actualLines = @(Format-LoadReadingLines -Load $load)

        # ASSERT
        $actualLines.Count | Should -Be $expectedCount
        Should -Invoke Format-MissingContentLine -Times 0
        Should -Invoke Format-ObjectChainLines -Times 0
    }
}
