BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-ModDriftChangeNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-VersionChangeLevel.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\New-ModDriftRow.ps1"

    function script:New-Mod {
        # One side's entry for a mod, as Read-SaveModList and Get-ModRoster give it.
        param(
            [string] $Name,
            [string] $Version
        )

        return [pscustomobject]@{ Id = 'alpha'; Name = $Name; Version = $Version; VersionInfo = "parts of $Version" }
    }
}

Describe 'New-ModDriftRow' {

    BeforeEach {

        Mock Get-ModDriftChangeNames {
            [pscustomobject]@{ Unchanged = 'UNCH'; Changed = 'CHG'; Missing = 'MISS'; Added = 'ADD' }
        }

        Mock Get-VersionChangeLevel { "level of $Installed against $Saved" }
    }

    It 'reads a mod at the same version text on both sides as unchanged, with no level' {

        $row = New-ModDriftRow -Saved (New-Mod 'Alpha' '1.0') -Installed (New-Mod 'Alpha' '1.0')

        $row.Change | Should -Be 'UNCH'
        $row.Level | Should -BeNullOrEmpty
        Should -Invoke Get-VersionChangeLevel -Times 0
    }

    It 'reads a mod whose version text differs as changed, ranked installed against saved' {

        $row = New-ModDriftRow -Saved (New-Mod 'Alpha' '1.0') -Installed (New-Mod 'Alpha' '1.1')

        $row.Id | Should -Be 'alpha'
        $row.SavedVersion | Should -Be '1.0'
        $row.InstalledVersion | Should -Be '1.1'
        $row.Change | Should -Be 'CHG'
        $row.Level | Should -Be 'level of parts of 1.1 against parts of 1.0'
    }

    # Version text is what a person sees; a rebuild the game reads as the same version can still ship other content.
    It 'reads version text that differs only in case as changed' {
        (New-ModDriftRow -Saved (New-Mod 'Alpha' '1.0b') -Installed (New-Mod 'Alpha' '1.0B')).Change | Should -Be 'CHG'
    }

    It 'reads a mod only the save has as missing, with its saved version' {

        $row = New-ModDriftRow -Saved (New-Mod 'Alpha' '1.0') -Installed $null

        $row.Change | Should -Be 'MISS'
        $row.SavedVersion | Should -Be '1.0'
        $row.InstalledVersion | Should -BeNullOrEmpty
        $row.Name | Should -Be 'Alpha'
    }

    It 'reads a mod only the install has as added, with its installed version and name' {

        $row = New-ModDriftRow -Saved $null -Installed (New-Mod 'Alpha Installed' '1.1')

        $row.Id | Should -Be 'alpha'
        $row.Change | Should -Be 'ADD'
        $row.SavedVersion | Should -BeNullOrEmpty
        $row.InstalledVersion | Should -Be '1.1'
        $row.Name | Should -Be 'Alpha Installed'
    }

    It 'names the mod as the save wrote it, else as installed' {

        (New-ModDriftRow -Saved (New-Mod ' Saved' '1.0') -Installed (New-Mod 'Installed' '1.0')).Name |
            Should -Be ' Saved'
        (New-ModDriftRow -Saved (New-Mod '' '1.0') -Installed (New-Mod 'Installed' '1.0')).Name |
            Should -Be 'Installed'
    }

    # formats\ModDriftRow.format.ps1xml selects the rows by this name, to print them as a table.
    It 'carries the type name the drift table view selects' {
        (New-ModDriftRow -Saved (New-Mod 'Alpha' '1.0') -Installed $null).PSObject.TypeNames[0] |
            Should -Be 'SaveMigrationTool.ModDriftRow'
    }
}
