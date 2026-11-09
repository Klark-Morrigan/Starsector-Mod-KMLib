BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\catalogue-cache\Get-TextHash.ps1"
}

Describe 'Get-TextHash' {

    # The FIPS 180-2 test vector for SHA-256.
    It 'hashes the text''s UTF-8 bytes with SHA-256, as lowercase hex' {

        # ARRANGE
        $expectedHash = 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad'

        # ACT
        $actualHash = Get-TextHash -Text 'abc'

        # ASSERT
        $actualHash | Should -BeExactly $expectedHash
    }
}
