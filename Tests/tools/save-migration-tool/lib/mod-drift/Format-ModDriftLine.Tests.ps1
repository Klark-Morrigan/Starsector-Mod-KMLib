BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\mod-drift\Format-ModDriftVersion.ps1"
    . "$libRoot\mod-drift\Format-VersionChangeLevel.ps1"
    . "$libRoot\mod-drift\Get-ModDriftChangeNames.ps1"
    . "$libRoot\mod-drift\Format-ModDriftLine.ps1"

    function script:New-Row {
        # One drift row as New-ModDriftRow builds it.
        param(
            [string] $Change,
            [string] $Name = ' Alpha'
        )

        return [pscustomobject]@{
            Id               = 'alpha'
            Name             = $Name
            SavedVersion     = '1.0'
            InstalledVersion = '1.1'
            Change           = $Change
            Level            = 'level-of-alpha'
        }
    }
}

Describe 'Format-ModDriftLine' {

    BeforeEach {

        Mock Get-ModDriftChangeNames {
            [pscustomobject]@{ Unchanged = 'UNCH'; Changed = 'CHG'; Missing = 'MISS'; Added = 'ADD' }
        }

        # Marks each painted value with its role, so a case pins the colour of every value it names.
        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ModDriftVersion { "v$Version" }
        Mock Format-VersionChangeLevel { "phrase for $Level" }
    }

    It 'says a changed mod with both versions and the level of its change' {

        # ARRANGE
        $row = New-Row -Change 'CHG'

        $expectedLine = '<Mod>Alpha (<Mod>alpha): v1.0 -> v1.1, phrase for level-of-alpha'

        # ACT
        $actualLine = Format-ModDriftLine -Row $row

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }

    It 'says a missing mod in red, with the version the save holds' {

        # ARRANGE
        $row = New-Row -Change 'MISS'

        $expectedLine = '<Mod>Alpha (<Mod>alpha): <Error>missing, saved at v1.0'

        # ACT
        $actualLine = Format-ModDriftLine -Row $row

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }

    It 'says an added mod in amber, with the installed version' {

        # ARRANGE
        $row = New-Row -Change 'ADD'

        $expectedLine = '<Mod>Alpha (<Mod>alpha): <Warning>added, installed at v1.1'

        # ACT
        $actualLine = Format-ModDriftLine -Row $row

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }

    It 'names a mod with no name by its ID alone' {

        # ARRANGE
        $row = New-Row -Change 'MISS' -Name ''

        $expectedLine = '<Mod>alpha: <Error>missing, saved at v1.0'

        # ACT
        $actualLine = Format-ModDriftLine -Row $row

        # ASSERT
        $actualLine | Should -Be $expectedLine
    }
}
