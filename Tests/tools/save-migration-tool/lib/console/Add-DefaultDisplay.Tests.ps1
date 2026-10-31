BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Add-DefaultDisplay.ps1"
}

Describe 'Add-DefaultDisplay' {

    BeforeEach {

        $script:Entry = [pscustomobject]@{
            Owner   = 'alpha'
            Folder  = 'C:\mods\Alpha'
            Name    = 'Alpha'
            Status  = 'parsed'
            Version = '1.2'
        }
    }

    It 'prints the given columns, in the given order, as a table' {

        # ARRANGE
        $expectedLines = @(
            'Owner Version Name'
            '----- ------- ----'
            'alpha 1.2     Alpha'
        )

        # ACT
        $actualText = Add-DefaultDisplay -InputObject $script:Entry -PropertyNames @('Owner', 'Version', 'Name') |
            Out-String

        # ASSERT
        @($actualText -split "`r?`n" | Where-Object { $_.Trim() }) | Should -Be $expectedLines
    }

    It 'keeps every field for a caller that serialises the object' {

        # ARRANGE
        $expectedJson = (
            '{"Owner":"alpha","Folder":"C:\\mods\\Alpha","Name":"Alpha","Status":"parsed","Version":"1.2"}')

        # ACT
        $actualJson = Add-DefaultDisplay -InputObject $script:Entry -PropertyNames @('Owner') | ConvertTo-Json -Compress

        # ASSERT
        $actualJson | Should -Be $expectedJson
    }
}
