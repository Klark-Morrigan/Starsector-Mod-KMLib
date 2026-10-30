BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Add-ModDefaultDisplay.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\ConvertTo-RosterEntry.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\ConvertTo-VersionInfo.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-version\Format-ModVersion.ps1"
}

Describe 'ConvertTo-RosterEntry' {

    BeforeEach {

        Mock Format-ModVersion { "formatted-$Version" }
        Mock ConvertTo-VersionInfo { "parts-$Version" }
        Mock Add-ModDefaultDisplay {
            $Mod | Add-Member -NotePropertyName 'Display' -NotePropertyValue 'added' -PassThru
        }

        $script:ModGroup = [pscustomobject]@{
            Winner     = [pscustomobject]@{
                Id            = 'alpha'
                Name          = 'Alpha'
                Version       = '1.2'
                Jars          = @('jars/alpha.jar', 'jars\..\lib.jar')
                SortString    = 'aa alpha'
                ReplacedPaths = @('data/hulls/ship_data.csv')
                Folder        = [System.IO.DirectoryInfo]::new('C:\Games\Starsector\mods\Alpha-1.2')
                Status        = 'parsed'
            }
            Superseded = @('older copy')
        }
    }

    It 'reports the winner with its formatted version, its version parts and the count of copies it beat' {

        # ARRANGE
        $expectedId = 'alpha'
        $expectedName = 'Alpha'
        $expectedVersion = 'formatted-1.2'
        $expectedVersionInfo = 'parts-1.2'
        $expectedDisplay = 'added'
        $expectedFolder = 'C:\Games\Starsector\mods\Alpha-1.2'
        $expectedStatus = 'parsed'
        $expectedSupersededCount = 1

        # ACT
        $actualEntry = ConvertTo-RosterEntry -ModGroup $script:ModGroup -StarsectorRoot 'C:\Games\Starsector'

        # ASSERT
        $actualEntry.Id | Should -Be $expectedId
        $actualEntry.Name | Should -Be $expectedName
        $actualEntry.Version | Should -Be $expectedVersion
        $actualEntry.VersionInfo | Should -Be $expectedVersionInfo
        $actualEntry.Display | Should -Be $expectedDisplay
        $actualEntry.Folder | Should -Be $expectedFolder
        $actualEntry.Status | Should -Be $expectedStatus
        $actualEntry.SupersededCount | Should -Be $expectedSupersededCount
    }

    It 'keeps the sort string and the replaced paths as mod_info.json writes them' {

        # ARRANGE
        $expectedSortString = 'aa alpha'
        $expectedReplacedPaths = @('data/hulls/ship_data.csv')

        # ACT
        $actualEntry = ConvertTo-RosterEntry -ModGroup $script:ModGroup -StarsectorRoot 'C:\Games\Starsector'

        # ASSERT
        $actualEntry.SortString | Should -Be $expectedSortString
        $actualEntry.ReplacedPaths | Should -Be $expectedReplacedPaths
    }

    It 'resolves each declared JAR against the mod''s folder' {

        # ARRANGE
        $expectedJars = @(
            'C:\Games\Starsector\mods\Alpha-1.2\jars\alpha.jar'
            'C:\Games\Starsector\mods\Alpha-1.2\lib.jar'
        )

        # ACT
        $actualEntry = ConvertTo-RosterEntry -ModGroup $script:ModGroup -StarsectorRoot 'C:\Games\Starsector'

        # ASSERT
        $actualEntry.Jars | Should -Be $expectedJars
    }

    It 'reports the folder relative to a root written as <Root>' -ForEach @(
        @{ Root = 'C:\Games\Starsector' }
        @{ Root = 'C:\Games\Starsector\' }
        @{ Root = 'c:\games\starsector' }
    ) {

        # ARRANGE
        $expectedRelativePath = 'mods\Alpha-1.2'

        # ACT
        $actualEntry = ConvertTo-RosterEntry -ModGroup $script:ModGroup -StarsectorRoot $Root

        # ASSERT
        $actualEntry.RelativePath | Should -Be $expectedRelativePath
    }
}
