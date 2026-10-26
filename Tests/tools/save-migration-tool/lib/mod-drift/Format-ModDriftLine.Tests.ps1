BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Format-ModDriftVersion.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-ModDriftChangeNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-VersionChangeLevelNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Format-ModDriftLine.ps1"

    function script:New-Row {
        # One drift row as New-ModDriftRow builds it.
        param(
            [string] $Change,
            [string] $Level,
            [string] $Name = ' Alpha'
        )

        return [pscustomobject]@{
            Id               = 'alpha'
            Name             = $Name
            SavedVersion     = '1.0'
            InstalledVersion = '1.1'
            Change           = $Change
            Level            = $Level
        }
    }
}

Describe 'Format-ModDriftLine' {

    BeforeEach {

        Mock Get-ModDriftChangeNames {
            [pscustomobject]@{ Unchanged = 'UNCH'; Changed = 'CHG'; Missing = 'MISS'; Added = 'ADD' }
        }

        Mock Get-VersionChangeLevelNames {
            [pscustomobject]@{ Major = 'MAJ'; Minor = 'MIN'; Patch = 'PAT' }
        }

        # Marks each painted value with its role, so a case pins the colour of every value it names.
        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ModDriftVersion { "v$Version" }
    }

    It 'says a change at level <Level> as <Phrase>' -ForEach @(
        @{ Level = 'MAJ'; Phrase = '<Error>major change' }
        @{ Level = 'MIN'; Phrase = '<Warning>minor change' }
        @{ Level = 'PAT'; Phrase = 'patch change' }
    ) {
        Format-ModDriftLine -Row (New-Row -Change 'CHG' -Level $Level) |
            Should -Be "<Mod>Alpha (<Mod>alpha): v1.0 -> v1.1, $Phrase"
    }

    It 'says a missing mod in red, with the version the save holds' {
        Format-ModDriftLine -Row (New-Row -Change 'MISS') |
            Should -Be '<Mod>Alpha (<Mod>alpha): <Error>missing, saved at v1.0'
    }

    It 'says an added mod in amber, with the installed version' {
        Format-ModDriftLine -Row (New-Row -Change 'ADD') |
            Should -Be '<Mod>Alpha (<Mod>alpha): <Warning>added, installed at v1.1'
    }

    It 'says an unchanged mod with its one version' {
        Format-ModDriftLine -Row (New-Row -Change 'UNCH') | Should -Be '<Mod>Alpha (<Mod>alpha): unchanged at v1.0'
    }

    It 'names a mod with no name by its ID alone' {
        Format-ModDriftLine -Row (New-Row -Change 'UNCH' -Name '') | Should -Be '<Mod>alpha: unchanged at v1.0'
    }
}
