BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\ConvertFrom-StarsectorJson.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Read-EnabledModIds.ps1"

    function script:New-ModsRoot {
        # Writes a mods folder under TestDrive, holding an enabled_mods.json when one is given, and returns it.
        param(
            [string] $Name,
            [switch] $HasEnabledFile
        )

        $modsRoot = Join-Path $TestDrive $Name

        New-Item -ItemType Directory -Path $modsRoot | Out-Null

        if ($HasEnabledFile) {
            New-Item -ItemType File -Path (Join-Path $modsRoot 'enabled_mods.json') | Out-Null
        }

        return $modsRoot
    }
}

Describe 'Read-EnabledModIds' {

    It 'reads the listed ids, skipping blank ones' {

        Mock ConvertFrom-StarsectorJson { [pscustomobject]@{ enabledMods = @('alpha', '', 'beta') } }

        $enabled = Read-EnabledModIds -ModsRoot (New-ModsRoot -Name 'listed' -HasEnabledFile)

        $enabled.Ids | Should -Be @('alpha', 'beta')
        $enabled.IsListRead | Should -BeTrue
    }

    It 'reads an empty list as read, with no ids' {

        Mock ConvertFrom-StarsectorJson { [pscustomobject]@{ enabledMods = @() } }

        $enabled = Read-EnabledModIds -ModsRoot (New-ModsRoot -Name 'empty' -HasEnabledFile)

        $enabled.Ids.Count | Should -Be 0
        $enabled.IsListRead | Should -BeTrue
    }

    It 'warns and reports no list when <Description>' -ForEach @(
        @{ Description = 'the file holds no list'; Reader = { [pscustomobject]@{ mods = @() } }
            Warning = 'enabled_mods.json unparseable, treating all mods as enabled: it holds no enabledMods list' }
        @{ Description = 'the file does not parse'; Reader = { throw 'bad json' }
            Warning = 'enabled_mods.json unparseable, treating all mods as enabled: bad json' }
    ) {

        Mock ConvertFrom-StarsectorJson $Reader

        $enabled = Read-EnabledModIds -ModsRoot (New-ModsRoot -Name "unread$($Warning.Length)" -HasEnabledFile) `
            -WarningVariable warnings -WarningAction SilentlyContinue

        $enabled.IsListRead | Should -BeFalse
        $warnings | ForEach-Object { "$_" } | Should -Be @($Warning)
    }

    It 'warns and reports no list when the file is missing' {

        $enabled = Read-EnabledModIds -ModsRoot (New-ModsRoot -Name 'missing') `
            -WarningVariable warnings -WarningAction SilentlyContinue

        $enabled.IsListRead | Should -BeFalse
        $warnings | ForEach-Object { "$_" } | Should -Be @('enabled_mods.json not found, treating all mods as enabled.')
    }
}
