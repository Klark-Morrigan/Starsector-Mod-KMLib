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

        # ARRANGE
        $saved = New-Mod 'Alpha' '1.0'
        $installed = New-Mod 'Alpha' '1.0'

        $expectedChange = 'UNCH'

        # ACT
        $actualRow = New-ModDriftRow -Saved $saved -Installed $installed

        # ASSERT
        $actualRow.Change | Should -Be $expectedChange
        $actualRow.Level | Should -BeNullOrEmpty
        Should -Invoke Get-VersionChangeLevel -Times 0
    }

    It 'reads a mod whose version text differs as changed, ranked installed against saved' {

        # ARRANGE
        $saved = New-Mod 'Alpha' '1.0'
        $installed = New-Mod 'Alpha' '1.1'

        $expectedId = 'alpha'
        $expectedSavedVersion = '1.0'
        $expectedInstalledVersion = '1.1'
        $expectedChange = 'CHG'
        $expectedLevel = 'level of parts of 1.1 against parts of 1.0'

        # ACT
        $actualRow = New-ModDriftRow -Saved $saved -Installed $installed

        # ASSERT
        $actualRow.Id | Should -Be $expectedId
        $actualRow.SavedVersion | Should -Be $expectedSavedVersion
        $actualRow.InstalledVersion | Should -Be $expectedInstalledVersion
        $actualRow.Change | Should -Be $expectedChange
        $actualRow.Level | Should -Be $expectedLevel
    }

    # Version text is what a person sees; a rebuild the game reads as the same version can still ship other content.
    It 'reads version text that differs only in case as changed' {

        # ARRANGE
        $saved = New-Mod 'Alpha' '1.0b'
        $installed = New-Mod 'Alpha' '1.0B'

        $expectedChange = 'CHG'

        # ACT
        $actualRow = New-ModDriftRow -Saved $saved -Installed $installed

        # ASSERT
        $actualRow.Change | Should -Be $expectedChange
    }

    It 'reads a mod only the save has as missing, with its saved version' {

        # ARRANGE
        $saved = New-Mod 'Alpha' '1.0'

        $expectedChange = 'MISS'
        $expectedSavedVersion = '1.0'
        $expectedName = 'Alpha'

        # ACT
        $actualRow = New-ModDriftRow -Saved $saved -Installed $null

        # ASSERT
        $actualRow.Change | Should -Be $expectedChange
        $actualRow.SavedVersion | Should -Be $expectedSavedVersion
        $actualRow.InstalledVersion | Should -BeNullOrEmpty
        $actualRow.Name | Should -Be $expectedName
    }

    It 'reads a mod only the install has as added, with its installed version and name' {

        # ARRANGE
        $installed = New-Mod 'Alpha Installed' '1.1'

        $expectedId = 'alpha'
        $expectedChange = 'ADD'
        $expectedInstalledVersion = '1.1'
        $expectedName = 'Alpha Installed'

        # ACT
        $actualRow = New-ModDriftRow -Saved $null -Installed $installed

        # ASSERT
        $actualRow.Id | Should -Be $expectedId
        $actualRow.Change | Should -Be $expectedChange
        $actualRow.SavedVersion | Should -BeNullOrEmpty
        $actualRow.InstalledVersion | Should -Be $expectedInstalledVersion
        $actualRow.Name | Should -Be $expectedName
    }

    It 'names the mod as the save wrote it, else as installed' {

        # ARRANGE
        $savedWithName = New-Mod ' Saved' '1.0'
        $savedWithoutName = New-Mod '' '1.0'
        $installed = New-Mod 'Installed' '1.0'

        $expectedSavedName = ' Saved'
        $expectedInstalledName = 'Installed'

        # ACT
        $actualSavedNameRow = New-ModDriftRow -Saved $savedWithName -Installed $installed
        $actualInstalledNameRow = New-ModDriftRow -Saved $savedWithoutName -Installed $installed

        # ASSERT
        $actualSavedNameRow.Name | Should -Be $expectedSavedName
        $actualInstalledNameRow.Name | Should -Be $expectedInstalledName
    }

    # formats\ModDriftRow.format.ps1xml selects the rows by this name, to print them as a table.
    It 'carries the type name the drift table view selects' {

        # ARRANGE
        $saved = New-Mod 'Alpha' '1.0'

        $expectedTypeName = 'SaveMigrationTool.ModDriftRow'

        # ACT
        $actualRow = New-ModDriftRow -Saved $saved -Installed $null

        # ASSERT
        $actualRow.PSObject.TypeNames[0] | Should -Be $expectedTypeName
    }
}
