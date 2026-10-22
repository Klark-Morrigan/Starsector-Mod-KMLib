BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Format-ModVersion.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Format-ModCopy.ps1"
}

Describe 'Format-ModCopy' {

    BeforeEach {

        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ModVersion { "v$Version" }
    }

    It 'names the copy''s folder as a path and its version as a mod''s' {

        $candidate = [pscustomobject]@{
            Folder  = [System.IO.DirectoryInfo]::new('C:\Games\Starsector\mods\Alpha-1.2')
            Version = '1.2'
        }

        Format-ModCopy -Candidate $candidate | Should -Be '<Path>Alpha-1.2 (<Mod>v1.2)'
    }
}
