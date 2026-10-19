# Integration tests for Get-ModRoster.ps1: the script and its lib\ functions run together, unmocked.
# Each lib function's own behaviour is pinned by its unit suite under lib\.
# Run with: Invoke-Pester -Path .\Get-ModRoster.IntegrationTests.Tests.ps1
#
# Each case builds a small install under TestDrive: a mods\ folder of hand-written mod_info.json files and,
# where the case needs one, an enabled_mods.json. No test reads a real install.

BeforeAll {

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

            $modFolder = Join-Path $modsRoot $folder
            New-Item -ItemType Directory -Path $modFolder -Force | Out-Null

            [System.IO.File]::WriteAllText((Join-Path $modFolder 'mod_info.json'), $ModInfoByFolder[$folder])
        }

        if ($EnabledModsJson) {
            [System.IO.File]::WriteAllText((Join-Path $modsRoot 'enabled_mods.json'), $EnabledModsJson)
        }

        return $root
    }

    function script:Invoke-ModRoster {
        <#
            Runs the script in-process, so coverage sees it, with stderr swapped for a buffer and warnings
            collected. The script writes its summary to the console's stderr, outside PowerShell's streams.
        #>
        param([hashtable] $Arguments)

        $stderrBuffer = [System.IO.StringWriter]::new()
        $originalStderr = [Console]::Error

        [Console]::SetError($stderrBuffer)

        try {
            $roster = & $script:ScriptPath @Arguments -WarningVariable warnings -WarningAction SilentlyContinue

        } finally {
            [Console]::SetError($originalStderr)
        }

        return [pscustomobject]@{
            Roster      = @($roster)
            Warnings    = @($warnings | ForEach-Object { "$_" })
            Diagnostics = @($stderrBuffer.ToString() -split "`r?`n" | Where-Object { $_ })
        }
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
            $script:Run.Roster.Id | Sort-Object | Should -Be @('alpha', 'beta')
        }

        It 'picks the newest copy of a mod, comparing versions numerically' {

            $alpha = $script:Run.Roster | Where-Object Id -EQ 'alpha'

            $alpha.Version | Should -Be '1.10.0'
            $alpha.RelativePath | Should -Be 'mods\Alpha-1.10.0'
            $alpha.SupersededCount | Should -Be 1
        }

        It 'reads the major, minor, patch version form' {
            ($script:Run.Roster | Where-Object Id -EQ 'beta').Version | Should -Be '2.0.3'
        }

        It 'logs what it read and decided to stderr' {
            $script:Run.Diagnostics | Should -Be @(
                "Get-ModRoster: read 4 mod folders under $script:InstallRoot\mods and 2 enabled IDs; " +
                'listing 2 mods (0 unparsed), skipping 1 disabled and 1 superseded copies'
            )
        }

        It 'lists disabled mods with -IncludeDisabled' {

            $run = Invoke-ModRoster @{ StarsectorRoot = $script:InstallRoot; IncludeDisabled = $true }

            $run.Roster.Id | Sort-Object | Should -Be @('alpha', 'beta', 'gamma')
        }
    }

    Context 'mod_info.json files the game reads but strict JSON does not' {

        BeforeAll {

            $script:Run = Invoke-ModRoster @{

                StarsectorRoot = (New-ModInstall -Name 'lenient' `
                    -EnabledModsJson '{ "enabledMods": ["a", "b", "c", "d", "e", "f", "g"] }' `
                    -ModInfoByFolder @{
                        'a' = "{`n  # a comment line`n  `"id`": `"a`", // a trailing comment`n  `"url`": `"https://example.com/a`",`n  `"version`": `"1.0`",`n}"
                        'b' = '{ "id": "b", "version": { "major": ''3'', "minor": ''1'', "patch": ''4'' } }'
                        'c' = '{ "id": "c", "version": { "major": 1, "minor": 2, "patch":a } }'
                        'd' = "$([char] 0xFEFF){ `"id`": `"d`", `"version`": `"0.5`" }"
                        'e' = '{ "id": "e", "name": "The \"Quoted\" # Mod", "version": "2.1" }'
                        'f' = '{ "id": "f", "version": 1.5 }'
                        'g' = '{ "id": "g" }'
                    })
            }
        }

        It 'parses comments, a URL holding //, and trailing commas' {
            ($script:Run.Roster | Where-Object Id -EQ 'a').Version | Should -Be '1.0'
        }

        It 'parses single-quoted version parts' {
            ($script:Run.Roster | Where-Object Id -EQ 'b').Version | Should -Be '3.1.4'
        }

        It 'parses a bare-word version part' {
            ($script:Run.Roster | Where-Object Id -EQ 'c').Version | Should -Be '1.2.a'
        }

        It 'parses a file opening with a byte order mark' {
            ($script:Run.Roster | Where-Object Id -EQ 'd').Version | Should -Be '0.5'
        }

        It 'keeps a # inside a string holding escaped quotes' {
            ($script:Run.Roster | Where-Object Id -EQ 'e').Version | Should -Be '2.1'
        }

        It 'reads a version written as a bare number' {
            ($script:Run.Roster | Where-Object Id -EQ 'f').Version | Should -Be '1.5'
        }

        It 'lists a mod with no version, with an empty version' {
            ($script:Run.Roster | Where-Object Id -EQ 'g').Version | Should -Be ''
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

            $broken = $script:Run.Roster | Where-Object Id -EQ 'Broken Mod'

            $broken.Status | Should -Be 'unparsed'
            $broken.Version | Should -Be ''
        }

        It 'keeps a mod_info.json with no id as unparsed' {
            ($script:Run.Roster | Where-Object Id -EQ 'No Id').Status | Should -Be 'unparsed'
        }

        It 'warns for each, naming the folder and the reason' {

            $warnings = @($script:Run.Warnings | Sort-Object)

            $warnings.Count | Should -Be 2
            $warnings[0] | Should -BeLike 'Unparseable mod_info.json, keeping in roster: Broken Mod (no repair produced valid JSON: *'
            $warnings[1] | Should -Be 'Unparseable mod_info.json, keeping in roster: No Id (no "id" field)'
        }
    }

    Context 'an install with no mod enabled' {

        It 'lists no mods, and does not warn' {

            $run = Invoke-ModRoster @{

                StarsectorRoot = (New-ModInstall -Name 'noneEnabled' -EnabledModsJson '{ "enabledMods": [] }' `
                    -ModInfoByFolder @{ 'a' = '{ "id": "a", "version": "1" }' })
            }

            $run.Roster.Count | Should -Be 0
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

            $run.Roster.Id | Should -Be @('a')
            $run.Warnings | Should -Be @('enabled_mods.json not found, treating all mods as enabled.')
        }

        It 'treats every mod as enabled when the file holds no list, and warns' {

            $run = Invoke-ModRoster @{

                StarsectorRoot = (New-ModInstall -Name 'noEnabledList' -EnabledModsJson '{ "mods": [] }' `
                    -ModInfoByFolder @{ 'a' = '{ "id": "a", "version": "1" }' })
            }

            $run.Roster.Id | Should -Be @('a')
            $run.Warnings | Should -Be @(
                'enabled_mods.json unparseable, treating all mods as enabled: it holds no enabledMods list'
            )
        }
    }

    Context 'folders without a mod_info.json' {

        It 'skips them' {

            $root = New-ModInstall -Name 'noInfo' -EnabledModsJson '{ "enabledMods": ["a"] }' `
                -ModInfoByFolder @{ 'a' = '{ "id": "a", "version": "1" }' }

            New-Item -ItemType Directory -Path (Join-Path $root 'mods\loose-assets') | Out-Null

            (Invoke-ModRoster @{ StarsectorRoot = $root }).Roster.Id | Should -Be @('a')
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
