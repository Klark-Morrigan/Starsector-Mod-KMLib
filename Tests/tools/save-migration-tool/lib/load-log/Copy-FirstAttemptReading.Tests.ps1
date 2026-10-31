BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log"

    . "$libRoot\Copy-FirstAttemptReading.ps1"
    . "$libRoot\New-FailureReading.ps1"

    function script:New-Reading {
        # A load or attempt holding the readings the mocked New-FailureReading names, and a field it does not.
        param(
            [string] $Cause,
            [string] $Missing,
            [string] $Other
        )

        return [pscustomobject]@{ Cause = $Cause; Missing = $Missing; Other = $Other }
    }
}

Describe 'Copy-FirstAttemptReading' {

    BeforeEach {
        Mock New-FailureReading { [ordered]@{ Cause = $null; Missing = $null } }
    }

    # The first attempt read campaign.xml, so its failure is the one the player met.
    It 'gives the load its first attempt''s readings, not a retry''s' {

        # ARRANGE
        $load = New-Reading
        $load | Add-Member -NotePropertyName 'Attempts' -NotePropertyValue @(
            (New-Reading -Cause 'first' -Missing 'first missing')
            (New-Reading -Cause 'retry' -Missing 'retry missing')
        )

        $expectedCause = 'first'
        $expectedMissing = 'first missing'

        # ACT
        $actualLoad = Copy-FirstAttemptReading -Load $load

        # ASSERT
        $actualLoad | Should -Be $load
        $actualLoad.Cause | Should -Be $expectedCause
        $actualLoad.Missing | Should -Be $expectedMissing
    }

    It 'copies only the readings New-FailureReading names' {

        # ARRANGE
        $load = New-Reading -Other 'the load''s own'
        $load | Add-Member -NotePropertyName 'Attempts' -NotePropertyValue @(New-Reading -Other 'the attempt''s own')

        $expectedOther = 'the load''s own'

        # ACT
        $actualLoad = Copy-FirstAttemptReading -Load $load

        # ASSERT
        $actualLoad.Other | Should -Be $expectedOther
    }
}
