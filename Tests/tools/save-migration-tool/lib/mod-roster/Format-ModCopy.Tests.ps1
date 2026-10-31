BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Format-ModCopy.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\Format-ModVersion.ps1"
}

Describe 'Format-ModCopy' {

    BeforeEach {

        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ModVersion { "v$Version" }
    }

    It 'names the copy''s folder as a path and its version as a mod''s' {

        # ARRANGE
        $candidate = [pscustomobject]@{
            Folder  = [System.IO.DirectoryInfo]::new('C:\Games\Starsector\mods\Alpha-1.2')
            Version = '1.2'
        }
        $expectedText = '<Path>Alpha-1.2 (<Mod>v1.2)'

        # ACT
        $actualText = Format-ModCopy -Candidate $candidate

        # ASSERT
        $actualText | Should -Be $expectedText
    }
}
