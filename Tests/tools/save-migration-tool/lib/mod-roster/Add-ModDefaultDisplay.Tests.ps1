BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Add-DefaultDisplay.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Add-ModDefaultDisplay.ps1"
}

Describe 'Add-ModDefaultDisplay' {

    It 'prints a mod as its ID, version and name, in that order' {

        # ARRANGE
        Mock Add-DefaultDisplay { "$($InputObject.Id) shown as $PropertyNames" }

        $mod = [pscustomobject]@{ Id = 'alpha' }

        $expectedDisplay = 'alpha shown as Id Version Name'

        # ACT
        $actualDisplay = Add-ModDefaultDisplay -Mod $mod

        # ASSERT
        $actualDisplay | Should -Be $expectedDisplay
    }
}
