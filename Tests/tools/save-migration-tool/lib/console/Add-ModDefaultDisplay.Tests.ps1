BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Add-ModDefaultDisplay.ps1"
}

Describe 'Add-ModDefaultDisplay' {

    BeforeEach {

        $script:Mod = [pscustomobject]@{
            Id      = 'alpha'
            Folder  = 'C:\mods\Alpha'
            Name    = 'Alpha'
            Status  = 'parsed'
            Version = '1.2'
        }
    }

    It 'prints the ID, version and name, in that order, as a table' {

        $text = Add-ModDefaultDisplay -Mod $script:Mod | Out-String

        $lines = @($text -split "`r?`n" | Where-Object { $_.Trim() })

        $lines | Should -Be @(
            'Id    Version Name'
            '--    ------- ----'
            'alpha 1.2     Alpha'
        )
    }

    It 'keeps every field for a caller that serialises the object' {

        $json = Add-ModDefaultDisplay -Mod $script:Mod | ConvertTo-Json -Compress

        $json | Should -Be '{"Id":"alpha","Folder":"C:\\mods\\Alpha","Name":"Alpha","Status":"parsed","Version":"1.2"}'
    }
}
