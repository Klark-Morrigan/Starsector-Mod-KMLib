# Integration tests for Get-ContentCatalogue.ps1: the script and its lib\ functions run together, unmocked.
# Each lib function's own behaviour is pinned by its unit suite under lib\.
# Run with: Invoke-Pester -Path .\Get-ContentCatalogue.IntegrationTests.Tests.ps1
#
# The install is built once under TestDrive: the game's own data under starsector-core\data, and three enabled mods.
# No test reads a real install.

BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\helpers\Invoke-ScriptCapturingOutput.ps1"
    . "$PSScriptRoot\helpers\New-TestFile.ps1"
    . "$PSScriptRoot\helpers\New-TestMod.ps1"

    # Tests\ mirrors the repo, so the script sits at the same relative path under the repo root.
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
    $script:ScriptPath = Join-Path $repoRoot 'tools\save-migration-tool\Get-ContentCatalogue.ps1'
    $script:InstallRoot = Join-Path $TestDrive 'install'

    function script:Invoke-ContentCatalogue {
        # Runs the script against the install with the given extra arguments; the rows are the run's Output.
        param([hashtable] $Arguments = @{})

        return Invoke-ScriptCapturingOutput -ScriptPath $script:ScriptPath -Arguments (
            @{ StarsectorRoot = $script:InstallRoot } + $Arguments)
    }

    function script:Format-CatalogueRows {
        # Writes each row as "<kind> <ID>: <owners>", in output order.
        param([object[]] $Rows)

        return @($Rows | ForEach-Object { "$($_.Kind) $($_.Id): $($_.Owners -join ', ')" })
    }

    # The game's own data: a hull with its row, a patched hull, a hull file with no row, a row with no file,
    # a skin, a weapon, a hullmod and a variant.
    New-TestFile -Name 'install\starsector-core\data\hulls\core_hull.ship' -Text '{ "hullId": "core_hull" }' | Out-Null
    New-TestFile -Name 'install\starsector-core\data\hulls\patched.ship' -Text '{ "hullId": "patched_hull" }' | Out-Null
    New-TestFile -Name 'install\starsector-core\data\hulls\rowless.ship' -Text '{ "hullId": "rowless_hull" }' | Out-Null
    New-TestFile -Name 'install\starsector-core\data\hulls\ship_data.csv' -Lines @(
        'name,id'
        'Core,core_hull'
        'Patched,patched_hull'
        '#Commented,commented_hull'
        'File-less,fileless_hull'
    ) | Out-Null
    New-TestFile -Name 'install\starsector-core\data\hulls\skins\core_skin.skin' -Text (
        '{ "baseHullId": "core_hull", "skinHullId": "core_skin" }') | Out-Null
    New-TestFile -Name 'install\starsector-core\data\weapons\core_gun.wpn' -Text (
        "{`n  # the weapon's ID`n  `"id`": `"core_gun`",`n  `"specClass`": `"projectile`",`n}") | Out-Null
    New-TestFile -Name 'install\starsector-core\data\weapons\weapon_data.csv' -Lines @('id,name', 'core_gun,Gun') |
        Out-Null
    New-TestFile -Name 'install\starsector-core\data\hullmods\hull_mods.csv' -Lines @('name,id', 'Core,core_mod') |
        Out-Null
    New-TestFile -Name 'install\starsector-core\data\variants\core_hull_Standard.variant' -Text (
        '{ "variantId": "core_hull_Standard", "hullId": "core_hull" }') | Out-Null

    # alpha patches the game's hull file without naming a hull, and adds a hull of its own.
    New-TestMod -StarsectorRoot $script:InstallRoot -Folder 'Alpha' -ModInfo '{ "id": "alpha", "name": "Alpha" }' |
        Out-Null
    New-TestFile -Name 'install\mods\Alpha\data\hulls\patched.ship' -Text '{ "hullName": "Patched by Alpha" }' |
        Out-Null
    New-TestFile -Name 'install\mods\Alpha\data\hulls\alpha_hull.ship' -Text '{ "hullId": "alpha_hull" }' | Out-Null
    New-TestFile -Name 'install\mods\Alpha\data\hulls\ship_data.csv' -Lines @('id,name', 'alpha_hull,Alpha') | Out-Null

    # Market conditions stand for every generic store type: alpha adds one, and a row for the game's own.
    New-TestFile -Name 'install\starsector-core\data\campaign\market_conditions.csv' -Lines @(
        'name,id'
        'Core,core_condition'
    ) | Out-Null
    New-TestFile -Name 'install\mods\Alpha\data\campaign\market_conditions.csv' -Lines @(
        'id,name'
        'alpha_condition,Alpha'
        'core_condition,Core restated'
    ) | Out-Null

    # LLI repeats a column name in its wing header, and keeps its fighter variants one folder down.
    New-TestMod -StarsectorRoot $script:InstallRoot -Folder 'LLI' -ModInfo '{ "id": "LLI", "name": "LLI" }' | Out-Null
    New-TestFile -Name 'install\mods\LLI\data\hulls\wing_data.csv' -Lines @(
        'id,variant,tags,02,02'
        'ssp_wing,ssp_wing_variant,"fighter,ssp_bp",,'
    ) | Out-Null
    New-TestFile -Name 'install\mods\LLI\data\variants\fighters\ssp_wing_variant.variant' -Text (
        '{ "variantId": "ssp_wing_variant", "hullId": "ssp_fighter" }') | Out-Null

    # Gamma replaces the hullmod spreadsheet, so the game's own hullmods are gone.
    New-TestMod -StarsectorRoot $script:InstallRoot -Folder 'Gamma' `
        -ModInfo '{ "id": "gamma", "name": "Gamma", "replace": ["data/hullmods/hull_mods.csv"] }' | Out-Null
    New-TestFile -Name 'install\mods\Gamma\data\hullmods\hull_mods.csv' -Lines @('id', 'gamma_mod') | Out-Null

    # factions.csv lists the .faction files; alpha lists one of its own and patches the game's without an ID.
    New-TestFile -Name 'install\starsector-core\data\world\factions\factions.csv' -Lines @(
        'faction'
        'data/world/factions/core.faction'
    ) | Out-Null
    # The game's own .faction files write their keys bare, as org.json takes them.
    New-TestFile -Name 'install\starsector-core\data\world\factions\core.faction' -Text "{`n`tid:`"core_faction`",`n}" |
        Out-Null
    New-TestFile -Name 'install\starsector-core\data\world\factions\unlisted.faction' -Text '{ "id": "unlisted" }' |
        Out-Null
    New-TestFile -Name 'install\mods\Alpha\data\world\factions\factions.csv' -Lines @(
        'faction'
        'data/world/factions/alpha.faction'
    ) | Out-Null
    New-TestFile -Name 'install\mods\Alpha\data\world\factions\alpha.faction' -Text '{ "id": "alpha_faction" }' |
        Out-Null
    New-TestFile -Name 'install\mods\Alpha\data\world\factions\core.faction' -Text '{ "color": [1, 2, 3] }' |
        Out-Null

    # planets.json is keyed by ID; the game skips a key whose value is not an object, and reads 10f as a number.
    New-TestFile -Name 'install\starsector-core\data\config\planets.json' -Text (
        '{ "core_planet": { "size": 10 }, "note": "not a planet" }') | Out-Null
    New-TestFile -Name 'install\mods\Alpha\data\config\planets.json' -Text '{ "alpha_planet": { "size": 10f } }' |
        Out-Null

    # A skill counts when skill_data.csv lists it or an aptitude names it as its effect, and only then.
    foreach ($skillId in 'core_skill', 'aptitude_skill', 'unlisted_skill') {

        New-TestFile -Name "install\starsector-core\data\characters\skills\$skillId.skill" -Text (
            "{ `"id`": `"$skillId`" }") | Out-Null
    }

    New-TestFile -Name 'install\starsector-core\data\characters\skills\skill_data.csv' -Lines @(
        'id,name'
        'core_skill,Core'
    ) | Out-Null
    New-TestFile -Name 'install\starsector-core\data\characters\skills\aptitude_data.csv' -Lines @(
        'id,effect_skill_id'
        'core_aptitude,aptitude_skill'
    ) | Out-Null

    # A description is keyed by its ID and type together.
    New-TestFile -Name 'install\starsector-core\data\strings\descriptions.csv' -Lines @(
        'id,type,text1'
        'core_hull,SHIP,A hull.'
    ) | Out-Null

    New-TestFile -Name 'install\mods\enabled_mods.json' -Text '{ "enabledMods": ["alpha", "LLI", "gamma"] }' | Out-Null
}

Describe 'Get-ContentCatalogue.IntegrationTests' {

    Context 'every kind, with no filter' {

        BeforeAll {
            $script:Run = Invoke-ContentCatalogue
        }

        # The variants the game builds in its own code, such as omen1, are named as a word and a digit; the
        # fixtures' IDs all hold an underscore, so the listing reads without the built-in ones.
        It 'lists each defined ID by kind, IDs in code-unit order, with its owners in the game''s search order' {

            $fixtureRows = $script:Run.Output | Where-Object { $_.Id -notmatch '^[a-z]+\d$' }

            Format-CatalogueRows -Rows $fixtureRows | Should -Be @(
                'Hull alpha_hull: alpha'
                'Hull alpha_hull_default_D: alpha'
                'Hull core_hull: vanilla'
                'Hull core_hull_default_D: vanilla'
                'Hull core_skin: vanilla'
                'Hull core_skin_default_D: vanilla'
                'Hull patched_hull: vanilla'
                'Hull patched_hull_default_D: vanilla'
                'Weapon core_gun: vanilla'
                'FighterWing ssp_wing: LLI'
                'Hullmod gamma_mod: gamma'
                'Variant alpha_hull_Hull: alpha'
                'Variant core_hull_Hull: vanilla'
                'Variant core_hull_Standard: vanilla'
                'Variant core_skin_Hull: vanilla'
                'Variant patched_hull_Hull: vanilla'
                'Variant ssp_wing_variant: LLI'
                'MarketCondition alpha_condition: alpha'
                'MarketCondition core_condition: alpha, vanilla'
                'Faction alpha_faction: alpha'
                'Faction core_faction: vanilla'
                'Skill aptitude_skill: vanilla'
                'Skill core_skill: vanilla'
                'Aptitude core_aptitude: vanilla'
                'Planet alpha_planet: alpha'
                'Planet core_planet: vanilla'
                'Description core_hull_SHIP: vanilla'
            )
        }

        It 'lists the 51 variants the game builds in its own code, owned by the game' {

            $builtInRows = @($script:Run.Output | Where-Object { $_.Id -match '^[a-z]+\d$' })

            $builtInRows.Count | Should -Be 51
            @($builtInRows | Where-Object { $_.Kind -ne 'Variant' -or "$($_.Owners)" -ne 'vanilla' }).Count |
                Should -Be 0
            $builtInRows.Id | Should -Contain 'omen1'
        }

        It 'leaves out a faction no factions.csv lists, and a skill no spreadsheet lists' {

            $ids = $script:Run.Output.Id

            $ids | Should -Not -Contain 'unlisted'
            $ids | Should -Not -Contain 'unlisted_skill'
        }

        It 'leaves out a planets.json key whose value is not an object' {
            $script:Run.Output.Id | Should -Not -Contain 'note'
        }

        It 'leaves out a hull whose file has no row, a row whose file is missing, and a commented row' {

            $hullIds = ($script:Run.Output | Where-Object Kind -EQ 'Hull').Id

            $hullIds | Should -Not -Contain 'rowless_hull'
            $hullIds | Should -Not -Contain 'fileless_hull'
            $hullIds | Should -Not -Contain 'commented_hull'
        }

        It 'says what it read, which spreadsheet a mod replaces, and what each kind holds, to stderr' {

            $script:Run.Diagnostics | Should -Contain (
                'Get-ContentCatalogue: cataloguing Hull, Weapon, FighterWing, Hullmod, Variant, ShipSystem, ' +
                'Projectile, MarketCondition, Industry, SpecialItem, Commodity, Ability, Submarket, Faction, Skill, ' +
                'Aptitude, Personality, PersonMission, BarEvent, CustomEntity, Planet, Terrain, BattleObjective, ' +
                'EngineStyle, HullStyle, CampaignEvent, Ping, IntelTag, ContactTag, Sound, MusicSet, Description, ' +
                'StarGenData, AgeGenData, ObjectiveGenData, LocationGenData, NameGenData, PlanetGenData, ' +
                'CategoryGenData, TerrainGenData, ConditionGenData, DropGroup, SalvageEntityGenData, Mission, Rule ' +
                "from 3 mods and the game's own data, in the game's search order")
            $script:Run.Diagnostics | Should -Contain (
                'Get-ContentCatalogue: gamma replaces data/hullmods/hull_mods.csv, so no other copy of it is read')
            @($script:Run.Diagnostics -like (
                'Get-ContentCatalogue: read 17 content files and 13 spreadsheets in * ms, skipping 0; ' +
                'defined: 8 Hull, 1 Weapon, 1 FighterWing, 1 Hullmod, 57 Variant, 0 ShipSystem, 0 Projectile, ' +
                '2 MarketCondition, 0 Industry, 0 SpecialItem, 0 Commodity, 0 Ability, 0 Submarket, 2 Faction, ' +
                '2 Skill, 1 Aptitude, 0 Personality, 0 PersonMission, 0 BarEvent, 0 CustomEntity, 2 Planet, ' +
                '0 Terrain, 0 BattleObjective, 0 EngineStyle, 0 HullStyle, 0 CampaignEvent, 0 Ping, 0 IntelTag, ' +
                '0 ContactTag, 0 Sound, 0 MusicSet, 1 Description, 0 StarGenData, 0 AgeGenData, 0 ObjectiveGenData, ' +
                '0 LocationGenData, 0 NameGenData, 0 PlanetGenData, 0 CategoryGenData, 0 TerrainGenData, ' +
                '0 ConditionGenData, 0 DropGroup, 0 SalvageEntityGenData, 0 Mission, 0 Rule, ' +
                '8 of them built by the game at startup')).Count | Should -Be 1
            $script:Run.Diagnostics[-1] | Should -Be 'Get-ContentCatalogue: listing 78 of 78 defined IDs'
        }
    }

    Context 'grouped by owner' {

        BeforeAll {
            $script:Run = Invoke-ContentCatalogue @{ GroupByOwner = $true }
        }

        It 'gives each owner one entry, in the game''s search order, with its name, version and ID count' {

            $script:Run.Output.Owner | Should -Be @('alpha', 'gamma', 'LLI', 'vanilla')
            $script:Run.Output.Name | Should -Be @('Alpha', 'Gamma', 'LLI', 'Starsector')
            $script:Run.Output.IdCount | Should -Be @(7, 1, 2, 69)
        }

        It 'holds an owner''s IDs by kind' {

            $alpha = $script:Run.Output[0]

            @($alpha.Ids.Keys) | Should -Be @('Hull', 'Variant', 'MarketCondition', 'Faction', 'Planet')
            $alpha.Ids['Hull'] | Should -Be @('alpha_hull', 'alpha_hull_default_D')
            $alpha.Ids['Variant'] | Should -Be @('alpha_hull_Hull')
            $alpha.Ids['MarketCondition'] | Should -Be @('alpha_condition', 'core_condition')
            $alpha.Ids['Faction'] | Should -Be @('alpha_faction')
            $alpha.Ids['Planet'] | Should -Be @('alpha_planet')
        }

        It 'says how many owners the IDs fall under' {
            $script:Run.Diagnostics[-1] | Should -Be 'Get-ContentCatalogue: listing 78 of 78 defined IDs under 4 owners'
        }
    }

    Context 'a file to write' {

        It 'writes the JSON there, creating its folder, and prints nothing to stdout' {

            $outFile = Join-Path $TestDrive 'out\nested\catalogue.json'

            $run = Invoke-ContentCatalogue @{ GroupByOwner = $true; OutFile = $outFile }

            $groups = Get-Content -Raw -LiteralPath $outFile | ConvertFrom-Json

            $run.Output.Count | Should -Be 0
            $groups.Owner | Should -Be @('alpha', 'gamma', 'LLI', 'vanilla')
            $groups[2].Ids.FighterWing | Should -Be @('ssp_wing')
            $run.Diagnostics[-1] | Should -Be "Get-ContentCatalogue: wrote the JSON to $outFile"
        }
    }

    Context 'filters' {

        It 'lists only the IDs matching a pattern, ignoring case' {

            $run = Invoke-ContentCatalogue @{ Id = @('SSP_*', 'core_gun') }

            Format-CatalogueRows -Rows $run.Output | Should -Be @(
                'Weapon core_gun: vanilla'
                'FighterWing ssp_wing: LLI'
                'Variant ssp_wing_variant: LLI'
            )
        }

        It 'lists the variants the game builds from hulls when only variants are asked for' {

            $run = Invoke-ContentCatalogue @{ Kind = @('Variant'); Id = @('*_Hull') }

            Format-CatalogueRows -Rows $run.Output | Should -Be @(
                'Variant alpha_hull_Hull: alpha'
                'Variant core_hull_Hull: vanilla'
                'Variant core_skin_Hull: vanilla'
                'Variant patched_hull_Hull: vanilla'
            )
        }

        It 'reads only the kinds asked for' {

            $run = Invoke-ContentCatalogue @{ Kind = @('FighterWing') }

            Format-CatalogueRows -Rows $run.Output | Should -Be @('FighterWing ssp_wing: LLI')
            @($run.Diagnostics -like '*read 0 content files and 1 spreadsheets*').Count | Should -Be 1
        }

        It 'fails on an unknown kind before reading anything, naming the kinds' {

            { Invoke-ContentCatalogue @{ Kind = @('Hull', 'Ship') } } |
                Should -Throw 'unknown kind: Ship; the kinds are Hull, Weapon, FighterWing, *, Mission, Rule'
        }
    }

    Context 'JSON' {

        It 'prints the rows as JSON, owners as a list' {

            $rows = (Invoke-ContentCatalogue @{ Id = @('ssp_wing'); AsJson = $true }).Output -join "`n" |
                ConvertFrom-Json

            $rows.Kind | Should -Be @('FighterWing')
            $rows.Owners | Should -Be @('LLI')
        }
    }
}
