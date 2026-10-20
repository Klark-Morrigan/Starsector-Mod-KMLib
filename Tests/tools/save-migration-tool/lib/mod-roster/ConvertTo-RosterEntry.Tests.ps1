BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Format-ModVersion.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\ConvertTo-RosterEntry.ps1"
}

Describe 'ConvertTo-RosterEntry' {

    BeforeEach {

        Mock Format-ModVersion { "formatted-$Version" }

        $script:ModGroup = [pscustomobject]@{
            Winner     = [pscustomobject]@{
                Id      = 'alpha'
                Version = '1.2'
                Folder  = [System.IO.DirectoryInfo]::new('C:\Games\Starsector\mods\Alpha-1.2')
                Status  = 'ok'
            }
            Superseded = @('older copy')
        }
    }

    It 'reports the winner with its formatted version and the count of copies it beat' {

        $entry = ConvertTo-RosterEntry -ModGroup $script:ModGroup -StarsectorRoot 'C:\Games\Starsector'

        $entry.Id | Should -Be 'alpha'
        $entry.Version | Should -Be 'formatted-1.2'
        $entry.Folder | Should -Be 'C:\Games\Starsector\mods\Alpha-1.2'
        $entry.Status | Should -Be 'ok'
        $entry.SupersededCount | Should -Be 1
    }

    It 'reports the folder relative to a root written as <Root>' -ForEach @(
        @{ Root = 'C:\Games\Starsector' }
        @{ Root = 'C:\Games\Starsector\' }
        @{ Root = 'c:\games\starsector' }
    ) {
        (ConvertTo-RosterEntry -ModGroup $script:ModGroup -StarsectorRoot $Root).RelativePath |
            Should -Be 'mods\Alpha-1.2'
    }
}
