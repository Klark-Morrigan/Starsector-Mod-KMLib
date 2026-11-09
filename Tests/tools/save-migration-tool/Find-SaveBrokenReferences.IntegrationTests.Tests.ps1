# Integration tests for Find-SaveBrokenReferences.ps1: the script and its lib\ functions run together, unmocked.
# Each lib function's own behaviour is pinned by its unit suite under lib\.
# Run with: Invoke-Pester -Path .\Find-SaveBrokenReferences.IntegrationTests.Tests.ps1
#
# The install is built once under TestDrive: the game's own data, one enabled mod, and two saves shaped as 0.98a
# writes campaign.xml, one naming content no installed mod defines and one naming only defined content.
# No test reads a real install. The cache goes to a temporary folder of its own, through LOCALAPPDATA,
# since Pester removes what a context adds to TestDrive when the context ends.

BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\helpers\Invoke-ScriptCapturingOutput.ps1"
    . "$PSScriptRoot\helpers\New-TestFile.ps1"
    . "$PSScriptRoot\helpers\New-TestMod.ps1"

    $script:OriginalLocalAppData = $env:LOCALAPPDATA
    $script:LocalAppData = Join-Path ([System.IO.Path]::GetTempPath()) "smt-broken-references-$([guid]::NewGuid())"
    $env:LOCALAPPDATA = $script:LocalAppData

    # Tests\ mirrors the repo, so the script sits at the same relative path under the repo root.
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
    $script:ScriptPath = Join-Path $repoRoot 'tools\save-migration-tool\Find-SaveBrokenReferences.ps1'
    $script:InstallRoot = Join-Path $TestDrive 'install'

    function script:Invoke-BrokenReferences {
        # Runs the script against the install with the given extra arguments; the rows are the run's Output.
        param([hashtable] $Arguments = @{})

        $allArguments = @{
            StarsectorRoot = $script:InstallRoot;
            SaveFolder = 'save_Broken_1' }

        foreach ($name in $Arguments.Keys) {
            $allArguments[$name] = $Arguments[$name]
        }

        return Invoke-ScriptCapturingOutput `
            -ScriptPath $script:ScriptPath `
            -Arguments $allArguments
    }

    function script:Format-BrokenRows {
        # Writes each row as "<kind> <ID> <uses>: <site> <uses>, ...", in output order.
        param([object[]] $Rows)

        return @($Rows | ForEach-Object {

            $sites = foreach ($site in $_.Sites.GetEnumerator()) {
                "$($site.Key) $($site.Value)"
            }
            "$($_.Kind) $($_.Id) $($_.UseCount): $($sites -join ', ')"
        })
    }

    # The game's own hull, weapon, hullmod, faction and personality, and a mod's hull.
    New-TestFile `
        -Name 'install\starsector-core\data\characters\personalities.csv' `
        -Lines @('id', 'steady') |
        Out-Null

    New-TestFile `
        -Name 'install\starsector-core\data\hulls\core_hull.ship' `
        -Text '{ "hullId": "core_hull" }' |
        Out-Null

    New-TestFile `
        -Name 'install\starsector-core\data\hulls\ship_data.csv' `
        -Lines @('id,name', 'core_hull,Core') |
        Out-Null

    New-TestFile `
        -Name 'install\starsector-core\data\weapons\core_gun.wpn' `
        -Text '{ "id": "core_gun" }' |
        Out-Null

    New-TestFile `
        -Name 'install\starsector-core\data\weapons\weapon_data.csv' `
        -Lines @('id,name', 'core_gun,Gun') |
        Out-Null

    New-TestFile `
        -Name 'install\starsector-core\data\hullmods\hull_mods.csv' `
        -Lines @('id,name', 'core_mod,Mod') |
        Out-Null

    New-TestFile `
        -Name 'install\starsector-core\data\world\factions\factions.csv' `
        -Lines @(
            'faction'
            'data/world/factions/core.faction'
        ) |
        Out-Null

    New-TestFile `
        -Name 'install\starsector-core\data\world\factions\core.faction' `
        -Text '{ "id": "core_faction" }' |
        Out-Null

    New-TestMod `
        -StarsectorRoot $script:InstallRoot `
        -Folder 'Alpha' `
        -ModInfo '{ "id": "alpha", "name": "Alpha", "description": "" }' |
        Out-Null

    New-TestFile `
        -Name 'install\mods\Alpha\data\hulls\alpha_hull.ship' `
        -Text '{ "hullId": "alpha_hull" }' |
        Out-Null

    New-TestFile `
        -Name 'install\mods\Alpha\data\hulls\ship_data.csv' `
        -Lines @('id,name', 'alpha_hull,Alpha') |
        Out-Null

    New-TestFile `
        -Name 'install\mods\enabled_mods.json' `
        -Text '{ "enabledMods": ["alpha"] }' |
        Out-Null

    # A save from when a removed mod, beta, was installed: its hull sits on a fleet member, in a faction's known
    # ships and as a d-hull; its gun in a slot and a cargo stack; its hullmod on a variant; its faction on a person.
    New-TestFile `
        -Name 'install\saves\save_Broken_1\campaign.xml' `
        -Text @'
<?xml version="1.0" ?>
<CampaignEngine z="1">
<Faction z="2">
<id>core_faction</id>
<knownShips z="3">
<st>core_hull</st>
<st>alpha_hull</st>
<st>beta_hull</st>
</knownShips>
</Faction>
<FMmbr z="4" sid="beta_hull_Standard" t="SHIP">
<c z="5" fid="beta_faction" pid="steady"></c>
<savedVariant z="6" hId="beta_hull_default_D" hVId="beta_hull_Hull">
<wpn z="7">
<e>
<st>WS 001</st>
<st>beta_gun</st>
</e>
<e>
<st>WS 002</st>
<st>core_gun</st>
</e>
</wpn>
<hM z="8">
<st>core_mod</st>
<st>beta_mod</st>
</hM>
</savedVariant>
</FMmbr>
<FMmbr z="9" sid="core_hull_Standard" t="SHIP">
<savedVariant z="10" hId="beta_hull"></savedVariant>
</FMmbr>
<CIStack z="11" t="WEAPONS">
<d cl="st">beta_gun</d>
</CIStack>
</CampaignEngine>
'@ | Out-Null

    # A person's skills are a JSON object the game writes; this one is not.
    New-TestFile `
        -Name 'install\saves\save_Unreadable_1\campaign.xml' `
        -Text @'
<?xml version="1.0" ?>
<CampaignEngine z="1"><c z="2"><stats z="3"><s>{"helmsmanship":</s></stats></c></CampaignEngine>
'@ | Out-Null

    New-TestFile `
        -Name 'install\saves\save_Clean_1\campaign.xml' `
        -Text @'
<?xml version="1.0" ?>
<CampaignEngine z="1">
<Faction z="2"><id>core_faction</id><knownShips z="3"><st>core_hull</st><st>alpha_hull</st></knownShips></Faction>
</CampaignEngine>
'@ | Out-Null
}

AfterAll {

    $env:LOCALAPPDATA = $script:OriginalLocalAppData
    Remove-Item `
        -LiteralPath $script:LocalAppData `
        -Recurse `
        -Force `
        -ErrorAction Ignore
}

Describe 'Find-SaveBrokenReferences.IntegrationTests' {

    Context 'a save naming content no installed mod defines' {

        BeforeAll {
            $script:Run = Invoke-BrokenReferences
        }

        # The game builds core_hull_default_D, but no file defines beta_hull, so neither its d-hull nor itself.
        It 'lists each missing ID by kind, with its uses at each site, and leaves out every defined one' {

            # ARRANGE
            $expectedRows = @(
                'Hull beta_hull 2: knownShips/st 1, savedVariant@hId 1'
                'Hull beta_hull_default_D 1: savedVariant@hId 1'
                'Weapon beta_gun 2: CIStack[t=WEAPONS]/d 1, wpn/e/st[2] 1'
                'Hullmod beta_mod 1: hM/st 1'
                'Faction beta_faction 1: *@fid 1'
            )

            # ACT
            $actualRows = Format-BrokenRows -Rows $script:Run.Output

            # ASSERT
            $actualRows | Should -Be $expectedRows
        }

        It 'says what it catalogued, what it read of the save, and how much is missing, to stderr' {

            # ARRANGE
            $expectedKindsLine =
                'Find-SaveBrokenReferences: cataloguing Hull, Weapon, FighterWing, Hullmod, MarketCondition, ' +
                'Industry, SpecialItem, Commodity, Ability, Submarket, Faction, Skill, Personality, CustomEntity, ' +
                "Planet, Terrain from 1 mods and the game's own data, in the game's search order"

            $expectedReadLinePattern =
                'Find-SaveBrokenReferences: read 24 elements of *campaign.xml in * ms: ' +
                '11 IDs named 13 times at 47 sites'

            $expectedLastLine =
                'Find-SaveBrokenReferences: 5 IDs are not defined, named 7 times: 2 Hull, 1 Weapon, 1 Hullmod, 1 Faction'

            $expectedReadLineCount = 1

            # ACT
            $actualDiagnostics = $script:Run.Diagnostics

            # ASSERT
            $actualDiagnostics |
                Should -Contain $expectedKindsLine

            @($actualDiagnostics `
                -like $expectedReadLinePattern).
                Count |
                Should -Be $expectedReadLineCount

            $actualDiagnostics[-1] |
                Should -Be $expectedLastLine
        }
    }

    Context 'a second run' {

        It 'takes every kind from the cache the first run filled, and lists the same rows' {

            # ARRANGE
            $expectedCachedLinePattern = 'Find-SaveBrokenReferences: loaded from the cache: Hull, Weapon, *, Terrain'
            $expectedCachedLineCount = 1
            $expectedRowCount = 5

            # ACT
            $actualRun = Invoke-BrokenReferences

            # ASSERT
            @($actualRun.Diagnostics `
                -like $expectedCachedLinePattern).
                Count |
                Should -Be $expectedCachedLineCount

            $actualRun.Output.Count |
                Should -Be $expectedRowCount
        }
    }

    Context 'a save naming only defined content' {

        It 'lists nothing, and says every ID is defined' {

            # ARRANGE
            $expectedOutputCount = 0
            $expectedLastLine = 'Find-SaveBrokenReferences: every ID the save names is defined'

            # ACT
            $actualRun = Invoke-BrokenReferences @{ SaveFolder = 'save_Clean_1' }

            # ASSERT
            $actualRun.Output.Count |
                Should -Be $expectedOutputCount

            $actualRun.Diagnostics[-1] |
                Should -Be $expectedLastLine
        }
    }

    Context 'a save holding a value a site cannot read' {

        It 'warns how many values each site could not read, and goes on' {

            # ARRANGE
            $expectedWarnings = @(
                "1 values at stats/s{keys} are not the JSON object the game writes there; their IDs are not counted")

            $expectedLastLine = 'Find-SaveBrokenReferences: every ID the save names is defined'

            # ACT
            $actualRun = Invoke-BrokenReferences @{ SaveFolder = 'save_Unreadable_1' }

            # ASSERT
            $actualRun.Warnings |
                Should -Be $expectedWarnings

            $actualRun.Diagnostics[-1] |
                Should -Be $expectedLastLine
        }
    }

    Context 'JSON' {

        It 'prints the rows as JSON, each site with its uses' {

            # ARRANGE
            $expectedIds = @(
                'beta_hull',
                'beta_hull_default_D',
                'beta_gun',
                'beta_mod',
                'beta_faction')

            $expectedHullSiteUses = 1

            # ACT
            $actualRun = Invoke-BrokenReferences @{ AsJson = $true }

            # ASSERT
            $actualRows = $actualRun.Output `
                -join "`n" |
                ConvertFrom-Json

            $actualRows.Id |
                Should -Be $expectedIds

            $actualRows[0].
                Sites.
                'knownShips/st' |
                Should -Be $expectedHullSiteUses
        }
    }

    Context 'a save that is not there' {

        # The catalogue can take a minute to read, so a mistyped save fails first.
        It 'fails naming the missing file, before reading the mods' {

            # ARRANGE
            $expectedMessagePattern = 'save not found: *saves\save_Missing_1\campaign.xml'

            # ACT
            $actualError = { Invoke-BrokenReferences @{ SaveFolder = 'save_Missing_1' } } |
                Should -Throw -PassThru

            # ASSERT
            $actualError.
                Exception.
                Message |
                Should -BeLike $expectedMessagePattern
        }
    }
}
