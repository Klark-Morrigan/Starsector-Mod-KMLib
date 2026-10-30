BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$libRoot\ConvertFrom-FailureTrace.ps1"
    . "$libRoot\ConvertFrom-ThrowableHeader.ps1"
    . "$libRoot\ConvertFrom-XStreamClassLine.ps1"

    # The parsers are mocked: "THROW|<type>|<message>" stands for a throwable header,
    # and "CLASS|<depth>|<class>" for a class line of XStream's debugging information.
    # Not mocked: the reader matches XStream's block opener itself.
    $script:DebugBlockStart = '---- Debugging information ----'
}

Describe 'ConvertFrom-FailureTrace' {

    BeforeEach {

        Mock ConvertFrom-ThrowableHeader {

            $parts = $Line -split '\|'

            if ($parts[0] -eq 'THROW') {
                [pscustomobject]@{ Type = $parts[1]; Message = $parts[2] }
            }
        }

        Mock ConvertFrom-XStreamClassLine {

            $parts = $Line -split '\|'

            if ($parts[0] -eq 'CLASS') {
                [pscustomobject]@{ Depth = [int] $parts[1]; Class = $parts[2] }
            }
        }
    }

    It 'takes the last throwable header as the root cause, past stack frames' {

        # ARRANGE
        $lines = @(
            'THROW|com.thoughtworks.xstream.converters.ConversionException|Failed calling method'
            '    at a.Loader.load(Unknown Source)'
            'THROW|java.lang.RuntimeException|Weapon spec [gone] not found!'
            '    at a.SpecStore.find(Unknown Source)'
        )

        $expectedCauseType = 'java.lang.RuntimeException'
        $expectedCause = 'Weapon spec [gone] not found!'

        # ACT
        $actualFailure = ConvertFrom-FailureTrace -Lines $lines

        # ASSERT
        $actualFailure.CauseType | Should -Be $expectedCauseType
        $actualFailure.Cause | Should -Be $expectedCause
    }

    It 'reads the object chain from the first debugging block only' {

        # ARRANGE
        $lines = @(
            $script:DebugBlockStart
            'CLASS|0|a.Failed'
            'CLASS|1|a.Container'
            'THROW|com.thoughtworks.xstream.converters.ConversionException|Failed calling method'
            $script:DebugBlockStart
            'CLASS|0|a.FromTheSecondBlock'
        )

        $expectedClasses = @('a.Failed', 'a.Container')
        $expectedDepths = @(0, 1)

        # ACT
        $actualFailure = ConvertFrom-FailureTrace -Lines $lines

        # ASSERT
        $actualFailure.ObjectChain.Class | Should -Be $expectedClasses
        $actualFailure.ObjectChain.Depth | Should -Be $expectedDepths
    }

    It 'ignores class lines outside a debugging block' {

        # ARRANGE
        $expectedChainLength = 0

        # ACT
        $actualFailure = ConvertFrom-FailureTrace -Lines @('CLASS|0|a.OutsideABlock')

        # ASSERT
        $actualFailure.ObjectChain.Count | Should -Be $expectedChainLength
    }

    It 'reads no cause and no chain from a trace naming neither' {

        # ARRANGE
        $expectedChainLength = 0

        # ACT
        $actualFailure = ConvertFrom-FailureTrace -Lines @('    at a.Loader.load(Unknown Source)')

        # ASSERT
        $actualFailure.Cause | Should -BeNullOrEmpty
        $actualFailure.ObjectChain.Count | Should -Be $expectedChainLength
    }
}
