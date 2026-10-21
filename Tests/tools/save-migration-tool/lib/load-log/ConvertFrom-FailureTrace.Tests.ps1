BeforeAll {

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

        $failure = ConvertFrom-FailureTrace -Lines @(
            'THROW|com.thoughtworks.xstream.converters.ConversionException|Failed calling method'
            '    at a.Loader.load(Unknown Source)'
            'THROW|java.lang.RuntimeException|Weapon spec [gone] not found!'
            '    at a.SpecStore.find(Unknown Source)'
        )

        $failure.CauseType | Should -Be 'java.lang.RuntimeException'
        $failure.Cause | Should -Be 'Weapon spec [gone] not found!'
    }

    It 'reads the object chain from the first debugging block only' {

        $failure = ConvertFrom-FailureTrace -Lines @(
            $script:DebugBlockStart
            'CLASS|0|a.Failed'
            'CLASS|1|a.Container'
            'THROW|com.thoughtworks.xstream.converters.ConversionException|Failed calling method'
            $script:DebugBlockStart
            'CLASS|0|a.FromTheSecondBlock'
        )

        $failure.ObjectChain.Class | Should -Be @('a.Failed', 'a.Container')
        $failure.ObjectChain.Depth | Should -Be @(0, 1)
    }

    It 'ignores class lines outside a debugging block' {

        $failure = ConvertFrom-FailureTrace -Lines @('CLASS|0|a.OutsideABlock')

        $failure.ObjectChain.Count | Should -Be 0
    }

    It 'reads no cause and no chain from a trace naming neither' {

        $failure = ConvertFrom-FailureTrace -Lines @('    at a.Loader.load(Unknown Source)')

        $failure.Cause | Should -BeNullOrEmpty
        $failure.ObjectChain.Count | Should -Be 0
    }
}
