# Integration tests for Get-ModRoster.ps1: the script and its lib\ functions run together, unmocked.
# Each lib function's own behaviour is pinned by its unit suite under lib\.
# Run with: Invoke-Pester -Path .\Get-ModRoster.IntegrationTests.Tests.ps1
#
# Each case builds a small install under TestDrive: a mods\ folder of hand-written mod_info.json files and,
# where the case needs one, an enabled_mods.json. No test reads a real install.

BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\helpers\Invoke-ScriptCapturingOutput.ps1"
    . "$PSScriptRoot\helpers\New-TestMod.ps1"

    # Tests\ mirrors the repo, so the script sits at the same relative path under the repo root.
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
    $script:ScriptPath = Join-Path $repoRoot 'tools\save-migration-tool\Get-ModRoster.ps1'

    function script:New-ModInstall {
        <#
            Writes an install root holding mods\<folder>\mod_info.json for each entry of -ModInfoByFolder,
            and mods\enabled_mods.json when -EnabledModsJson is given. Returns the root.
        #>
        param(
            [string] $Name,
            [hashtable] $ModInfoByFolder,
            [string] $EnabledModsJson
        )

        $root = Join-Path $TestDrive $Name
        $modsRoot = Join-Path $root 'mods'

        New-Item -ItemType Directory -Path $modsRoot -Force | Out-Null

        foreach ($folder in $ModInfoByFolder.Keys) {
            New-TestMod -StarsectorRoot $root -Folder $folder -ModInfo $ModInfoByFolder[$folder] | Out-Null
        }

        if ($EnabledModsJson) {
            [System.IO.File]::WriteAllText((Join-Path $modsRoot 'enabled_mods.json'), $EnabledModsJson)
        }

        return $root
    }

    function script:Invoke-ModRoster {
        # Runs the script on the given arguments; the roster is the run's Output.
        param([hashtable] $Arguments)

        return Invoke-ScriptCapturingOutput -ScriptPath $script:ScriptPath -Arguments $Arguments
    }
}

Describe 'Get-ModRoster.IntegrationTests' {

    Context 'an install with enabled, disabled and superseded mods' {

        BeforeAll {

            $script:InstallRoot = New-ModInstall -Name 'mixed' -EnabledModsJson '{ "enabledMods": ["alpha", "beta"] }' `
                -ModInfoByFolder @{
                    'Alpha'           = '{ "id": "alpha", "name": "Alpha", "version": "1.9.0" }'
                    'Alpha-1.10.0'    = '{ "id": "alpha", "name": "Alpha", "version": "1.10.0" }'
                    'beta'            = '{ "id": "beta", "version": { "major": 2, "minor": 0, "patch": 3 } }'
                    'gamma'           = '{ "id": "gamma", "version": "0.1" }'
                }

            $script:Run = Invoke-ModRoster @{ StarsectorRoot = $script:InstallRoot }
        }

        It 'lists only the enabled mods' {
            $script:Run.Output.Id | Sort-Object | Should -Be @('alpha', 'beta')
        }

        It 'picks the newest copy of a mod, comparing versions numerically' {

            $alpha = $script:Run.Output | Where-Object Id -EQ 'alpha'

            $alpha.Version | Should -Be '1.10.0'
            $alpha.RelativePath | Should -Be 'mods\Alpha-1.10.0'
            $alpha.SupersededCount | Should -Be 1
        }

        It 'prints as a table of ID, version and name, as Get-SaveModDrift.ps1 prints a save''s mods' {

            $lines = @($script:Run.Output | Out-String -Width 200) -split "`r?`n" | Where-Object { $_.Trim() }

            $lines[0] | Should -Match '^Id\s+Version\s+Name$'
            @($lines -match '^alpha\s+1\.10\.0\s+Alpha$').Count | Should -Be 1
        }

        It 'reads the major, minor, patch version form' {
            ($script:Run.Output | Where-Object Id -EQ 'beta').Version | Should -Be '2.0.3'
        }

        It 'gives each mod the version parts the game compares by, from either version form' {

            $alphaParts = ($script:Run.Output | Where-Object Id -EQ 'alpha').VersionInfo
            $betaParts = ($script:Run.Output | Where-Object Id -EQ 'beta').VersionInfo

            "$($alphaParts.Major)|$($alphaParts.Minor)|$($alphaParts.Patch)" | Should -Be '1|10|0'
            "$($betaParts.Major)|$($betaParts.Minor)|$($betaParts.Patch)" | Should -Be '2|0|3'
        }

        It 'logs which copy won over which, and what it read and decided, to stderr' {
            $script:Run.Diagnostics | Should -Be @(
                'Get-ModRoster: alpha: Alpha-1.10.0 (1.10.0) wins over Alpha (1.9.0)'
                ("Get-ModRoster: read 4 mod folders under $script:InstallRoot\mods and 2 enabled IDs; " +
                    'listing 2 mods (0 unparsed), skipping 1 disabled and 1 superseded copies')
            )
        }

        It 'lists disabled mods with -IncludeDisabled' {

            $run = Invoke-ModRoster @{ StarsectorRoot = $script:InstallRoot; IncludeDisabled = $true }

            $run.Output.Id | Sort-Object | Should -Be @('alpha', 'beta', 'gamma')
        }

        It 'reports the same relative path for a root written with a trailing slash' {

            $run = Invoke-ModRoster @{ StarsectorRoot = "$script:InstallRoot\" }

            ($run.Output | Where-Object Id -EQ 'alpha').RelativePath | Should -Be 'mods\Alpha-1.10.0'
        }
    }

    Context 'two copies of a mod at the same version' {

        It 'picks the first by folder name, and says the version tied' {

            $root = New-ModInstall -Name 'tie' -EnabledModsJson '{ "enabledMods": ["beta"] }' -ModInfoByFolder @{
                'Beta B' = '{ "id": "beta", "version": "1.0" }'
                'Beta A' = '{ "id": "beta", "version": "1.0" }'
            }

            $run = Invoke-ModRoster @{ StarsectorRoot = $root }

            $run.Output.RelativePath | Should -Be @('mods\Beta A')
            $run.Diagnostics | Should -Contain (
                'Get-ModRoster: beta: Beta A (1.0) wins over Beta B (1.0), ' +
                'the same version, so first by folder name')
        }
    }

    Context 'a mod that declares JARs' {

        It 'lists each declared JAR as a full path under the mod''s folder' {

            $root = New-ModInstall -Name 'jars' -EnabledModsJson '{ "enabledMods": ["a"] }' -ModInfoByFolder @{
                'A Mod' = '{ "id": "a", "version": "1", "jars": ["jars/a.jar", "jars/lib/b.jar"] }'
            }

            (Invoke-ModRoster @{ StarsectorRoot = $root }).Output.Jars | Should -Be @(
                "$root\mods\A Mod\jars\a.jar"
                "$root\mods\A Mod\jars\lib\b.jar"
            )
        }
    }

    Context 'a mod_info.json the game reads but strict JSON does not' {

        It 'reads it as the game does: # comments, trailing commas and single-quoted values' {

            $modInfo = "{`n  # a comment line`n  `"id`": `"a`", # a trailing comment`n" +
                "  `"version`": { `"major`": '1', `"minor`": 2, },`n}"

            $run = Invoke-ModRoster @{

                StarsectorRoot = (New-ModInstall -Name 'lenient' -EnabledModsJson '{ "enabledMods": ["a"] }' `
                    -ModInfoByFolder @{ 'a' = $modInfo })
            }

            $run.Output.Version | Should -Be '1.2'
            $run.Warnings.Count | Should -Be 0
        }
    }

    Context 'a mod_info.json nothing can parse' {

        BeforeAll {

            $script:Run = Invoke-ModRoster @{

                StarsectorRoot = (New-ModInstall -Name 'unparsed' -EnabledModsJson '{ "enabledMods": [] }' `
                    -ModInfoByFolder @{
                        'Broken Mod' = '{ "id": "broken", '
                        'No Id'      = '{ "name": "Nameless" }'
                    })
            }
        }

        It 'keeps the folder in the roster as unparsed, though no enabled ID names it' {

            $broken = $script:Run.Output | Where-Object Id -EQ 'Broken Mod'

            $broken.Status | Should -Be 'unparsed'
            $broken.Version | Should -Be ''
        }

        It 'keeps a mod_info.json with no "id" field as unparsed' {
            ($script:Run.Output | Where-Object Id -EQ 'No Id').Status | Should -Be 'unparsed'
        }

        It 'warns for each, naming the folder and the reason' {

            $warnings = @($script:Run.Warnings | Sort-Object)

            $warnings.Count | Should -Be 2
            $warnings[0] | Should -BeLike (
                'No mod ID read from mod_info.json, keeping in roster: Broken Mod ' +
                '(not valid JSON, even with unquoted values quoted: *')
            $warnings[1] | Should -Be 'No mod ID read from mod_info.json, keeping in roster: No Id (no "id" field)'
        }
    }

    Context 'an install with no mod enabled' {

        It 'lists no mods, and does not warn' {

            $run = Invoke-ModRoster @{

                StarsectorRoot = (New-ModInstall -Name 'noneEnabled' -EnabledModsJson '{ "enabledMods": [] }' `
                    -ModInfoByFolder @{ 'a' = '{ "id": "a", "version": "1" }' })
            }

            $run.Output.Count | Should -Be 0
            $run.Warnings.Count | Should -Be 0
        }
    }

    Context 'an install with no usable enabled_mods.json' {

        It 'treats every mod as enabled when the file is missing, and warns' {

            $run = Invoke-ModRoster @{

                StarsectorRoot = (New-ModInstall -Name 'noEnabledFile' -ModInfoByFolder @{
                    'a' = '{ "id": "a", "version": "1" }'
                })
            }

            $run.Output.Id | Should -Be @('a')
            $run.Warnings | Should -Be @('enabled_mods.json not found, treating all mods as enabled.')
        }

        It 'treats every mod as enabled when the file holds no list, and warns' {

            $run = Invoke-ModRoster @{

                StarsectorRoot = (New-ModInstall -Name 'noEnabledList' -EnabledModsJson '{ "mods": [] }' `
                    -ModInfoByFolder @{ 'a' = '{ "id": "a", "version": "1" }' })
            }

            $run.Output.Id | Should -Be @('a')
            $run.Warnings | Should -Be @(
                'enabled_mods.json unparseable, treating all mods as enabled: it holds no enabledMods list'
            )
        }
    }

    Context 'folders without a mod_info.json' {

        It 'skips them, naming them in the log' {

            $root = New-ModInstall -Name 'noInfo' -EnabledModsJson '{ "enabledMods": ["a"] }' `
                -ModInfoByFolder @{ 'a' = '{ "id": "a", "version": "1" }' }

            New-Item -ItemType Directory -Path (Join-Path $root 'mods\loose-assets') | Out-Null

            $run = Invoke-ModRoster @{ StarsectorRoot = $root }

            $run.Output.Id | Should -Be @('a')
            $run.Diagnostics[0] | Should -Be 'Get-ModRoster: skipped 1 folders with no mod_info.json: loose-assets'
        }
    }

    Context 'an install with no mods folder' {

        It 'fails naming the path' {

            $root = Join-Path $TestDrive 'empty'

            New-Item -ItemType Directory -Path $root | Out-Null

            { Invoke-ModRoster @{ StarsectorRoot = $root } } | Should -Throw "mods directory not found: $root\mods"
        }
    }
}
