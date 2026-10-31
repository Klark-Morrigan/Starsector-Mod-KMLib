BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\readers\Invoke-CachedRead.ps1"

    function script:Read-TestFile {
        # The read under the cache; a case mocks it to count its calls.
        param([string] $Path)
    }
}

Describe 'Invoke-CachedRead' {

    BeforeEach {

        $script:Results = [System.Collections.Generic.Dictionary[string, object]]::new()

        Mock Read-TestFile { "parsed $Path" }
    }

    It 'runs the read with its parameters the first time a key is asked for, and returns what it read' {

        # ARRANGE
        $expectedValue = 'parsed a.json'

        # ACT
        $actualValue = Invoke-CachedRead -Results $script:Results -Key 'a' -Command 'Read-TestFile' `
            -Parameters @{ Path = 'a.json' }

        # ASSERT
        $actualValue | Should -Be $expectedValue
        Should -Invoke Read-TestFile -ParameterFilter { $Path -eq 'a.json' } -Times 1 -Exactly
    }

    It 'hands a later caller of the same key the first result, without reading again' {

        # ARRANGE
        Invoke-CachedRead -Results $script:Results -Key 'a' -Command 'Read-TestFile' -Parameters @{ Path = 'a.json' } |
            Out-Null

        $expectedValue = 'parsed a.json'

        # ACT
        $actualValue = Invoke-CachedRead -Results $script:Results -Key 'a' -Command 'Read-TestFile' `
            -Parameters @{ Path = 'other.json' }

        # ASSERT
        $actualValue | Should -Be $expectedValue
        Should -Invoke Read-TestFile -Times 1 -Exactly
    }

    It 'reads each key apart' {

        # ARRANGE
        Invoke-CachedRead -Results $script:Results -Key 'a' -Command 'Read-TestFile' -Parameters @{ Path = 'a.json' } |
            Out-Null

        $expectedValue = 'parsed b.json'
        $expectedReadCount = 2

        # ACT
        $actualValue = Invoke-CachedRead -Results $script:Results -Key 'b' -Command 'Read-TestFile' `
            -Parameters @{ Path = 'b.json' }

        # ASSERT
        $actualValue | Should -Be $expectedValue
        Should -Invoke Read-TestFile -Times $expectedReadCount -Exactly
    }

    # Every kind reading a broken file skips it for the same reason.
    It 'fails every caller of a key whose read failed with the same message, reading it once' {

        # ARRANGE
        Mock Read-TestFile { throw 'not valid JSON' }

        $expectedMessage = 'not valid JSON'

        # ACT
        $actualFirstError = {
            Invoke-CachedRead -Results $script:Results -Key 'a' -Command 'Read-TestFile' -Parameters @{ Path = 'a' }
        } | Should -Throw -PassThru
        $actualSecondError = {
            Invoke-CachedRead -Results $script:Results -Key 'a' -Command 'Read-TestFile' -Parameters @{ Path = 'a' }
        } | Should -Throw -PassThru

        # ASSERT
        $actualFirstError.Exception.Message | Should -Be $expectedMessage
        $actualSecondError.Exception.Message | Should -Be $expectedMessage
        Should -Invoke Read-TestFile -Times 1 -Exactly
    }
}
