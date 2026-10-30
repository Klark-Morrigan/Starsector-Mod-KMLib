BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\ConvertFrom-StarsectorJson.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\install\Get-InstallPathNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Read-EnabledModIds.ps1"

    function script:New-ModsRoot {
        # Writes a mods folder under TestDrive, holding an enabled_mods.json when one is given, and returns it.
        param(
            [string] $Name,
            [switch] $HasEnabledFile
        )

        $modsRoot = Join-Path $TestDrive $Name

        New-Item -ItemType Directory -Path $modsRoot -Force | Out-Null

        if ($HasEnabledFile) {
            New-TestFile -Name "$Name\enabled_mods.json" | Out-Null
        }

        return $modsRoot
    }
}

Describe 'Read-EnabledModIds' {

    BeforeEach {
        Mock Get-InstallPathNames { [pscustomobject]@{ EnabledModsFile = 'enabled_mods.json' } }
    }

    It 'reads the listed IDs, skipping blank ones' {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson { [ordered]@{ enabledMods = @('alpha', '', 'beta') } }

        $modsRoot = New-ModsRoot -Name 'listed' -HasEnabledFile
        $expectedIds = @('alpha', 'beta')

        # ACT
        $actualEnabled = Read-EnabledModIds -ModsRoot $modsRoot

        # ASSERT
        $actualEnabled.Ids | Should -Be $expectedIds
        $actualEnabled.IsListRead | Should -BeTrue
    }

    # An install with no mod enabled holds an empty list, which must not read as no list.
    It 'reads an empty list as read, with no IDs' {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson { [ordered]@{ enabledMods = @() } }

        $modsRoot = New-ModsRoot -Name 'empty' -HasEnabledFile
        $expectedIdCount = 0

        # ACT
        $actualEnabled = Read-EnabledModIds -ModsRoot $modsRoot

        # ASSERT
        $actualEnabled.Ids.Count | Should -Be $expectedIdCount
        $actualEnabled.IsListRead | Should -BeTrue
    }

    It 'warns and reports no list when <Description>' -ForEach @(
        @{ Description = 'the file holds no list'; Reader = { [ordered]@{ mods = @() } }
            Warning = 'enabled_mods.json unparseable, treating all mods as enabled: it holds no enabledMods list' }
        # An [ordered] literal matches keys case-blind, where the parser's hashtables do not.
        @{ Description = 'the list key differs in case'
            Reader = { '{ "EnabledMods": ["alpha"] }' | ConvertFrom-Json -AsHashtable }
            Warning = 'enabled_mods.json unparseable, treating all mods as enabled: it holds no enabledMods list' }
        @{ Description = 'the root is not an object'; Reader = { , @('alpha') }
            Warning = 'enabled_mods.json unparseable, treating all mods as enabled: it holds no enabledMods list' }
        @{ Description = 'the file does not parse'; Reader = { throw 'bad json' }
            Warning = 'enabled_mods.json unparseable, treating all mods as enabled: bad json' }
    ) {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson $Reader

        $modsRoot = New-ModsRoot -Name "unread $Description" -HasEnabledFile
        $expectedWarnings = @($Warning)

        # ACT
        $actualEnabled = Read-EnabledModIds -ModsRoot $modsRoot -WarningVariable actualWarnings `
            -WarningAction SilentlyContinue

        # ASSERT
        $actualEnabled.IsListRead | Should -BeFalse
        $actualWarnings | ForEach-Object { "$_" } | Should -Be $expectedWarnings
    }

    It 'warns and reports no list when the file is missing' {

        # ARRANGE
        $modsRoot = New-ModsRoot -Name 'missing'
        $expectedWarnings = @('enabled_mods.json not found, treating all mods as enabled.')

        # ACT
        $actualEnabled = Read-EnabledModIds -ModsRoot $modsRoot -WarningVariable actualWarnings `
            -WarningAction SilentlyContinue

        # ASSERT
        $actualEnabled.IsListRead | Should -BeFalse
        $actualWarnings | ForEach-Object { "$_" } | Should -Be $expectedWarnings
    }
}
