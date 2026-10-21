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
                Jars    = @('jars/alpha.jar', 'jars\..\lib.jar')
                Folder  = [System.IO.DirectoryInfo]::new('C:\Games\Starsector\mods\Alpha-1.2')
                Status  = 'parsed'
            }
            Superseded = @('older copy')
        }
    }

    It 'reports the winner with its formatted version and the count of copies it beat' {

        $entry = ConvertTo-RosterEntry -ModGroup $script:ModGroup -StarsectorRoot 'C:\Games\Starsector'

        $entry.Id | Should -Be 'alpha'
        $entry.Version | Should -Be 'formatted-1.2'
        $entry.Folder | Should -Be 'C:\Games\Starsector\mods\Alpha-1.2'
        $entry.Status | Should -Be 'parsed'
        $entry.SupersededCount | Should -Be 1
    }

    It 'resolves each declared jar against the mod''s folder' {

        (ConvertTo-RosterEntry -ModGroup $script:ModGroup -StarsectorRoot 'C:\Games\Starsector').Jars | Should -Be @(
            'C:\Games\Starsector\mods\Alpha-1.2\jars\alpha.jar'
            'C:\Games\Starsector\mods\Alpha-1.2\lib.jar'
        )
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
