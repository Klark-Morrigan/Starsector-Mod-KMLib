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

        New-Item `
            -ItemType Directory `
            -Path $modsRoot `
            -Force |
            Out-Null

        if ($HasEnabledFile) {

            New-TestFile `
                -Name "$Name\enabled_mods.json" |
                Out-Null
        }

        return $modsRoot
    }
}

Describe 'Read-EnabledModIds' {

    BeforeEach {

        Mock Get-InstallPathNames { [pscustomobject]@{
            EnabledModsFile = 'enabled_mods.json' } }
    }

    It 'reads the listed IDs, skipping blank ones' {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson { [ordered]@{
            enabledMods = @('alpha', '', 'beta') } }

        $modsRoot = New-ModsRoot `
            -Name 'listed' `
            -HasEnabledFile

        $expectedIds = @('alpha', 'beta')

        # ACT
        $actualIds = @(Read-EnabledModIds `
            -ModsRoot $modsRoot)

        # ASSERT
        $actualIds |
            Should -Be $expectedIds
    }

    # An install with no mod enabled holds an empty list.
    It 'reads an empty list as no IDs, and does not warn' {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson { [ordered]@{
            enabledMods = @() } }

        $modsRoot = New-ModsRoot `
            -Name 'empty' `
            -HasEnabledFile

        $expectedIdCount = 0
        $expectedWarningCount = 0

        # ACT
        $actualIds = @(Read-EnabledModIds `
            -ModsRoot $modsRoot `
            -WarningVariable actualWarnings `
            -WarningAction SilentlyContinue)

        # ASSERT
        $actualIds.
            Count |
            Should -Be $expectedIdCount

        @($actualWarnings).
            Count |
            Should -Be $expectedWarningCount
    }

    # The game's read stops at an empty entry, keeping the IDs before it.
    It 'keeps the IDs before an empty entry, and warns naming its position' {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson { [ordered]@{
            enabledMods = @('alpha', $null, 'beta') } }

        $modsRoot = New-ModsRoot `
            -Name 'part-read' `
            -HasEnabledFile

        $expectedIds = @('alpha')
        $expectedWarnings = @(
            'enabled_mods.json has an empty entry at position 2, so the game loads only the IDs before it.')

        # ACT
        $actualIds = @(Read-EnabledModIds `
            -ModsRoot $modsRoot `
            -WarningVariable actualWarnings `
            -WarningAction SilentlyContinue)

        # ASSERT
        $actualIds |
            Should -Be $expectedIds

        $actualWarnings |
            ForEach-Object { "$_" } |
            Should -Be $expectedWarnings
    }

    It 'gives no IDs, and warns, when <Description>' -ForEach @(
        @{
            Description = 'the file holds no list';
            Reader = { [ordered]@{ mods = @() } }
            Warning = 'enabled_mods.json unparsable, so the game loads no mods: it holds no enabledMods list' }
        @{ # An [ordered] literal matches keys case-blind, where the parser's hashtables do not.
            Description = 'the list key differs in case'
            Reader = { '{ "EnabledMods": ["alpha"] }' | ConvertFrom-Json -AsHashtable }
            Warning = 'enabled_mods.json unparsable, so the game loads no mods: it holds no enabledMods list' }
        @{ # The game reads the list with getJSONArray, which throws on a lone string.
            Description = 'the list is one string';
            Reader = { [ordered]@{ enabledMods = 'alpha' } }
            Warning = 'enabled_mods.json unparsable, so the game loads no mods: it holds no enabledMods list' }
        @{
            Description = 'the root is not an object';
            Reader = { , @('alpha') }
            Warning = 'enabled_mods.json unparsable, so the game loads no mods: it holds no enabledMods list' }
        @{
            Description = 'the file does not parse';
            Reader = { throw 'bad json' }
            Warning = 'enabled_mods.json unparsable, so the game loads no mods: bad json' }
    ) {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson $Reader

        $modsRoot = New-ModsRoot `
            -Name "unread $Description" `
            -HasEnabledFile

        $expectedIdCount = 0
        $expectedWarnings = @($Warning)

        # ACT
        $actualIds = @(Read-EnabledModIds `
            -ModsRoot $modsRoot `
            -WarningVariable actualWarnings `
            -WarningAction SilentlyContinue)

        # ASSERT
        $actualIds.
            Count |
            Should -Be $expectedIdCount

        $actualWarnings |
            ForEach-Object { "$_" } |
            Should -Be $expectedWarnings
    }

    It 'gives no IDs, and warns, when the file is missing' {

        # ARRANGE
        $modsRoot = New-ModsRoot -Name 'missing'
        $expectedIdCount = 0
        $expectedWarnings = @('enabled_mods.json not found, so the game loads no mods.')

        # ACT
        $actualIds = @(Read-EnabledModIds `
            -ModsRoot $modsRoot `
            -WarningVariable actualWarnings `
            -WarningAction SilentlyContinue)

        # ASSERT
        $actualIds.
            Count |
            Should -Be $expectedIdCount

        $actualWarnings |
            ForEach-Object { "$_" } |
            Should -Be $expectedWarnings
    }
}
