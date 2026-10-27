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

        $text = Add-DefaultDisplay -InputObject $script:Entry -PropertyNames @('Owner', 'Version', 'Name') | Out-String

        $lines = @($text -split "`r?`n" | Where-Object { $_.Trim() })

        $lines | Should -Be @(
            'Owner Version Name'
            '----- ------- ----'
            'alpha 1.2     Alpha'
        )
    }

    It 'keeps every field for a caller that serialises the object' {

        $json = Add-DefaultDisplay -InputObject $script:Entry -PropertyNames @('Owner') | ConvertTo-Json -Compress

        $json | Should -Be (
            '{"Owner":"alpha","Folder":"C:\\mods\\Alpha","Name":"Alpha","Status":"parsed","Version":"1.2"}')
    }
}
