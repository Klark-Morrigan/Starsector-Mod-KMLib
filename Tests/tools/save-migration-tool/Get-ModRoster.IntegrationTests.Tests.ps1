# Integration tests for Get-ModRoster.ps1: the script and its lib\ functions run together, unmocked.
# Each lib function's own behaviour is pinned by its unit suite under lib\.
# Run with: Invoke-Pester -Path .\Get-ModRoster.IntegrationTests.Tests.ps1
#
# Each case builds a small install under TestDrive: a mods\ folder of hand-written mod_info.json files and,
# where the case needs one, an enabled_mods.json. No test reads a real install.
# A context sharing one run makes it in BeforeAll; each case's act then reads its part of that run.

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
                    'Alpha'                 = '{ "id": "alpha", "name": "Alpha", "version": "1.9.0" }'
                    '[Repack] Alpha-1.10.0' = '{ "id": "alpha", "name": "Alpha", "version": "1.10.0" }'
                    'beta'                  = '{ "id": "beta", "version": { "major": 2, "minor": 0, "patch": 3 } }'
                    'gamma'                 = '{ "id": "gamma", "version": "0.1" }'
                }

            $script:Run = Invoke-ModRoster @{ StarsectorRoot = $script:InstallRoot }
        }

        It 'lists only the enabled mods' {

            # ARRANGE
            $expectedIds = @('alpha', 'beta')

            # ACT
            $actualIds = $script:Run.Output.Id | Sort-Object

            # ASSERT
            $actualIds | Should -Be $expectedIds
        }

        # The game keeps the first copy of a mod the file system lists, whatever its version.
        # NTFS lists "[" after letters, where a sort by name puts it first.
        It 'picks the copy whose folder comes first, though another copy is newer' {

            # ARRANGE
            $expectedVersion = '1.9.0'
            $expectedRelativePath = 'mods\Alpha'
            $expectedSupersededCount = 1

            # ACT
            $actualAlpha = $script:Run.Output | Where-Object Id -EQ 'alpha'

            # ASSERT
            $actualAlpha.Version | Should -Be $expectedVersion
            $actualAlpha.RelativePath | Should -Be $expectedRelativePath
            $actualAlpha.SupersededCount | Should -Be $expectedSupersededCount
        }

        It 'prints as a table of ID, version and name, as Get-SaveModDrift.ps1 prints a save''s mods' {

            # ARRANGE
            $expectedHeaderPattern = '^Id\s+Version\s+Name$'
            $expectedAlphaRowPattern = '^alpha\s+1\.9\.0\s+Alpha$'
            $expectedAlphaRowCount = 1

            # ACT
            $actualLines = @($script:Run.Output | Out-String -Width 200) -split "`r?`n" | Where-Object { $_.Trim() }

            # ASSERT
            $actualLines[0] | Should -Match $expectedHeaderPattern
            @($actualLines -match $expectedAlphaRowPattern).Count | Should -Be $expectedAlphaRowCount
        }

        It 'reads the major, minor, patch version form' {

            # ARRANGE
            $expectedVersion = '2.0.3'

            # ACT
            $actualVersion = ($script:Run.Output | Where-Object Id -EQ 'beta').Version

            # ASSERT
            $actualVersion | Should -Be $expectedVersion
        }

        It 'gives each mod the version parts the game compares by, from either version form' {

            # ARRANGE
            $expectedAlphaParts = '1|9|0'
            $expectedBetaParts = '2|0|3'

            # ACT
            $actualAlphaParts = ($script:Run.Output | Where-Object Id -EQ 'alpha').VersionInfo
            $actualBetaParts = ($script:Run.Output | Where-Object Id -EQ 'beta').VersionInfo

            # ASSERT
            "$($actualAlphaParts.Major)|$($actualAlphaParts.Minor)|$($actualAlphaParts.Patch)" |
                Should -Be $expectedAlphaParts
            "$($actualBetaParts.Major)|$($actualBetaParts.Minor)|$($actualBetaParts.Patch)" |
                Should -Be $expectedBetaParts
        }

        It 'logs which copy won over which, and what it read and decided, to stderr' {

            # ARRANGE
            $expectedDiagnostics = @(
                ('Get-ModRoster: alpha: Alpha (1.9.0) wins over [Repack] Alpha-1.10.0 (1.10.0), ' +
                    'coming first in folder order')
                ("Get-ModRoster: read 4 mod folders under $script:InstallRoot\mods and 2 enabled IDs; " +
                    'listing 2 mods (0 unparsed), skipping 1 disabled and 1 superseded copies')
            )

            # ACT
            $actualDiagnostics = $script:Run.Diagnostics

            # ASSERT
            $actualDiagnostics | Should -Be $expectedDiagnostics
        }

        It 'lists disabled mods with -IncludeDisabled' {

            # ARRANGE
            $expectedIds = @('alpha', 'beta', 'gamma')

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = $script:InstallRoot; IncludeDisabled = $true }

            # ASSERT
            $actualRun.Output.Id | Sort-Object | Should -Be $expectedIds
        }

        It 'reports the same relative path for a root written with a trailing slash' {

            # ARRANGE
            $expectedRelativePath = 'mods\Alpha'

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = "$script:InstallRoot\" }

            # ASSERT
            ($actualRun.Output | Where-Object Id -EQ 'alpha').RelativePath | Should -Be $expectedRelativePath
        }
    }

    # The game skips a copy whose mod_info.json it cannot read, so that copy never claims the mod's ID.
    Context 'a first copy of a mod nothing can parse' {

        It 'picks the next copy as the winner, and keeps the first apart as unparsed' {

            # ARRANGE
            $root = New-ModInstall -Name 'unparsedFirst' -EnabledModsJson '{ "enabledMods": ["beta"] }' `
                -ModInfoByFolder @{
                    'Beta A' = '{ "id": "beta", '
                    'Beta B' = '{ "id": "beta", "version": "1.0" }'
                }
            $expectedBetaPath = 'mods\Beta B'
            $expectedBetaSupersededCount = 0
            $expectedUnparsedStatus = 'unparsed'
            $expectedSummaryPattern = '*listing 2 mods (1 unparsed), skipping 0 disabled and 0 superseded copies'

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = $root }

            # ASSERT
            ($actualRun.Output | Where-Object Id -EQ 'beta').RelativePath | Should -Be $expectedBetaPath
            ($actualRun.Output | Where-Object Id -EQ 'beta').SupersededCount | Should -Be $expectedBetaSupersededCount
            ($actualRun.Output | Where-Object Id -EQ 'Beta A').Status | Should -Be $expectedUnparsedStatus
            @($actualRun.Diagnostics)[-1] | Should -BeLike $expectedSummaryPattern
        }
    }

    # The game compares mod IDs with String.equals, so IDs differing only in case name two mods.
    Context 'mod IDs that differ only in case' {

        It 'lists two copies whose IDs differ in case as two mods, neither superseding the other' {

            # ARRANGE
            $root = New-ModInstall -Name 'idCase' -EnabledModsJson '{ "enabledMods": ["foo", "Foo"] }' `
                -ModInfoByFolder @{
                    'Upper' = '{ "id": "Foo", "version": "2.0" }'
                    'Lower' = '{ "id": "foo", "version": "1.0" }'
                }
            $expectedUpperPath = 'mods\Upper'
            $expectedLowerPath = 'mods\Lower'
            $expectedSupersededCounts = @(0, 0)

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = $root }

            # ASSERT
            ($actualRun.Output | Where-Object Id -CEQ 'Foo').RelativePath | Should -Be $expectedUpperPath
            ($actualRun.Output | Where-Object Id -CEQ 'foo').RelativePath | Should -Be $expectedLowerPath
            $actualRun.Output.SupersededCount | Should -Be $expectedSupersededCounts
        }

        It 'leaves out a mod whose ID enabled_mods.json spells in another case' {

            # ARRANGE
            $root = New-ModInstall -Name 'enabledCase' -EnabledModsJson '{ "enabledMods": ["Foo"] }' `
                -ModInfoByFolder @{ 'a' = '{ "id": "foo", "version": "1" }' }
            $expectedCount = 0
            $expectedSummaryPattern = '*listing 0 mods (0 unparsed), skipping 1 disabled*'

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = $root }

            # ASSERT
            $actualRun.Output.Count | Should -Be $expectedCount
            @($actualRun.Diagnostics)[-1] | Should -BeLike $expectedSummaryPattern
        }
    }

    Context 'a mod that declares JARs' {

        It 'lists each declared JAR as a full path under the mod''s folder' {

            # ARRANGE
            $root = New-ModInstall -Name 'jars' -EnabledModsJson '{ "enabledMods": ["a"] }' -ModInfoByFolder @{
                'A Mod' = '{ "id": "a", "version": "1", "jars": ["jars/a.jar", "jars/lib/b.jar"] }'
            }
            $expectedJars = @(
                "$root\mods\A Mod\jars\a.jar"
                "$root\mods\A Mod\jars\lib\b.jar"
            )

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = $root }

            # ASSERT
            $actualRun.Output.Jars | Should -Be $expectedJars
        }
    }

    Context 'a mod_info.json the game reads but strict JSON does not' {

        It 'reads it as the game does: # comments, trailing commas and single-quoted values' {

            # ARRANGE
            $modInfo = "{`n  # a comment line`n  `"id`": `"a`", # a trailing comment`n" +
                "  `"version`": { `"major`": '1', `"minor`": 2, },`n}"
            $root = New-ModInstall -Name 'lenient' -EnabledModsJson '{ "enabledMods": ["a"] }' `
                -ModInfoByFolder @{ 'a' = $modInfo }
            $expectedVersion = '1.2'
            $expectedWarningCount = 0

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = $root }

            # ASSERT
            $actualRun.Output.Version | Should -Be $expectedVersion
            $actualRun.Warnings.Count | Should -Be $expectedWarningCount
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

            # ARRANGE
            $expectedStatus = 'unparsed'
            $expectedVersion = ''

            # ACT
            $actualBroken = $script:Run.Output | Where-Object Id -EQ 'Broken Mod'

            # ASSERT
            $actualBroken.Status | Should -Be $expectedStatus
            $actualBroken.Version | Should -Be $expectedVersion
        }

        It 'keeps a mod_info.json with no "id" field as unparsed' {

            # ARRANGE
            $expectedStatus = 'unparsed'

            # ACT
            $actualStatus = ($script:Run.Output | Where-Object Id -EQ 'No Id').Status

            # ASSERT
            $actualStatus | Should -Be $expectedStatus
        }

        It 'warns for each, naming the folder and the reason' {

            # ARRANGE
            $expectedWarningCount = 2
            $expectedBrokenWarning = 'No mod ID read from mod_info.json, keeping in roster: Broken Mod ' +
                "(not valid JSON, even with org.json's leniencies repaired: *"
            $expectedNoIdWarning = 'No mod ID read from mod_info.json, keeping in roster: No Id (no "id" field)'

            # ACT
            $actualWarnings = @($script:Run.Warnings | Sort-Object)

            # ASSERT
            $actualWarnings.Count | Should -Be $expectedWarningCount
            $actualWarnings[0] | Should -BeLike $expectedBrokenWarning
            $actualWarnings[1] | Should -Be $expectedNoIdWarning
        }
    }

    Context 'an install with no mod enabled' {

        It 'lists no mods, and does not warn' {

            # ARRANGE
            $root = New-ModInstall -Name 'noneEnabled' -EnabledModsJson '{ "enabledMods": [] }' `
                -ModInfoByFolder @{ 'a' = '{ "id": "a", "version": "1" }' }
            $expectedCount = 0
            $expectedWarningCount = 0

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = $root }

            # ASSERT
            $actualRun.Output.Count | Should -Be $expectedCount
            $actualRun.Warnings.Count | Should -Be $expectedWarningCount
        }
    }

    Context 'an install with no usable enabled_mods.json' {

        It 'treats every mod as enabled when the file is missing, and warns' {

            # ARRANGE
            $root = New-ModInstall -Name 'noEnabledFile' -ModInfoByFolder @{
                'a' = '{ "id": "a", "version": "1" }'
            }
            $expectedIds = @('a')
            $expectedWarnings = @('enabled_mods.json not found, treating all mods as enabled.')

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = $root }

            # ASSERT
            $actualRun.Output.Id | Should -Be $expectedIds
            $actualRun.Warnings | Should -Be $expectedWarnings
        }

        It 'treats every mod as enabled when the file holds no list, and warns' {

            # ARRANGE
            $root = New-ModInstall -Name 'noEnabledList' -EnabledModsJson '{ "mods": [] }' `
                -ModInfoByFolder @{ 'a' = '{ "id": "a", "version": "1" }' }
            $expectedIds = @('a')
            $expectedWarnings = @(
                'enabled_mods.json unparseable, treating all mods as enabled: it holds no enabledMods list'
            )

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = $root }

            # ASSERT
            $actualRun.Output.Id | Should -Be $expectedIds
            $actualRun.Warnings | Should -Be $expectedWarnings
        }
    }

    Context 'folders without a mod_info.json' {

        It 'skips them, naming them in the log' {

            # ARRANGE
            $root = New-ModInstall -Name 'noInfo' -EnabledModsJson '{ "enabledMods": ["a"] }' `
                -ModInfoByFolder @{ 'a' = '{ "id": "a", "version": "1" }' }

            New-Item -ItemType Directory -Path (Join-Path $root 'mods\loose-assets') | Out-Null

            $expectedIds = @('a')
            $expectedSkipLine = 'Get-ModRoster: skipped 1 folders with no mod_info.json: loose-assets'

            # ACT
            $actualRun = Invoke-ModRoster @{ StarsectorRoot = $root }

            # ASSERT
            $actualRun.Output.Id | Should -Be $expectedIds
            $actualRun.Diagnostics[0] | Should -Be $expectedSkipLine
        }
    }

    Context 'an install with no mods folder' {

        It 'fails naming the path' {

            # ARRANGE
            $root = Join-Path $TestDrive 'empty'

            New-Item -ItemType Directory -Path $root | Out-Null

            $expectedMessage = "mods directory not found: $root\mods"

            # ACT
            $actualError = { Invoke-ModRoster @{ StarsectorRoot = $root } } | Should -Throw -PassThru

            # ASSERT
            $actualError.Exception.Message | Should -Be $expectedMessage
        }
    }
}
