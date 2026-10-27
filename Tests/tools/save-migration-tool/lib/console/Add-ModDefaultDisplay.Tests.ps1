BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Add-DefaultDisplay.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Add-ModDefaultDisplay.ps1"
}

Describe 'Add-ModDefaultDisplay' {

    It 'prints a mod as its ID, version and name, in that order' {

        Mock Add-DefaultDisplay { "$($InputObject.Id) shown as $PropertyNames" }

        Add-ModDefaultDisplay -Mod ([pscustomobject]@{ Id = 'alpha' }) | Should -Be 'alpha shown as Id Version Name'
    }
}
