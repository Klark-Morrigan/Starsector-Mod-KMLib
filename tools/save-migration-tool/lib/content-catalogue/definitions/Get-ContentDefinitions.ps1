# Where the game reads each kind of content it defines by ID, as of 0.98a, and what it takes as defining an ID.
# Each kind is a list of parts, as New-ContentPart describes them, and an ID one part defines is defined.
# Spreadsheets key their rows by an id column unless a source says otherwise.
#
# The game also builds content at startup that no file names, from the IDs the files define.
# Every hull and skin gets a d-hull, under its ID plus _default_D, unless its ID already ends so,
# and an empty variant under its ID plus _Hull. A d-hull gets neither.
function Get-ContentDefinitions {

    $kinds = Get-ContentKindNames

    $dHullRule = [pscustomobject]@{ FromKind = $kinds.Hull; Suffix = '_default_D'; IsSuffixedBaseSkipped = $true }
    $emptyVariantRule = [pscustomobject]@{ FromKind = $kinds.Hull; Suffix = '_Hull'; IsSuffixedBaseSkipped = $false }

    $hullFiles = New-ContentFileSet -Folder 'data/hulls' -Ending 'ship' -IdKey 'hullId'
    $skinFiles = New-ContentFileSet -Folder 'data/hulls/skins' -Ending 'skin' -IdKey 'skinHullId' -HasSubfolders
    $weaponFiles = New-ContentFileSet -Folder 'data/weapons' -Ending 'wpn' -IdKey 'id'
    $variantFiles = New-ContentFileSet -Folder 'data/variants' -Ending 'variant' -IdKey 'variantId' -HasSubfolders
    $shipSystemFiles = New-ContentFileSet -Folder 'data/shipsystems' -Ending 'system' -IdKey 'id'
    $skillFiles = New-ContentFileSet -Folder 'data/characters/skills' -Ending 'skill' -IdKey 'id'

    # Weapons and projectiles a ship system mounts share the weapon and projectile stores.
    $systemWeaponFiles = New-ContentFileSet -Folder 'data/shipsystems/wpn' -Ending 'wpn' -IdKey 'id'
    $projectileFiles = New-ContentFileSet -Folder 'data/weapons/proj' -Ending 'proj' -IdKey 'id'
    $systemProjectileFiles = New-ContentFileSet -Folder 'data/shipsystems/proj' -Ending 'proj' -IdKey 'id'

    # factions.csv lists every .faction file the game loads; one it does not list is never read.
    $factionFiles = New-ContentFileSet -IdKey 'id' -ListedBy (
        New-SpreadsheetSource -Path 'data/world/factions/factions.csv' -KeyColumns @('faction'))

    # A skill counts when skill_data.csv lists it, or when an aptitude names it as its effect.
    $skillRows = New-SpreadsheetSource -Path 'data/characters/skills/skill_data.csv'
    $aptitudeEffectRows = New-SpreadsheetSource -Path 'data/characters/skills/aptitude_data.csv' `
        -IdColumn 'effect_skill_id'

    # planet_gen_data.csv holds three kinds of row, told apart by their type column.
    $planetGenPath = 'data/campaign/procgen/planet_gen_data.csv'
    $planetGenKeyColumns = @('id', 'type', 'category')

    return @(
        # Ships and what they mount. A hull, weapon or ship system needs its file and its row;
        # a skin copies its base hull's row, so it needs none.
        New-ContentKind -Kind $kinds.Hull -Generated @($dHullRule) -Parts @(
            New-ContentPart -FileSets @($hullFiles) `
                -Spreadsheets @(New-SpreadsheetSource -Path 'data/hulls/ship_data.csv')
            New-ContentPart -FileSets @($skinFiles)
        )
        New-ContentKind -Kind $kinds.Weapon -Parts @(
            New-ContentPart -FileSets @($weaponFiles, $systemWeaponFiles) `
                -Spreadsheets @(New-SpreadsheetSource -Path 'data/weapons/weapon_data.csv')
        )
        New-ContentKind -Kind $kinds.FighterWing -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/hulls/wing_data.csv')
        )
        New-ContentKind -Kind $kinds.Hullmod -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/hullmods/hull_mods.csv')
        )
        New-ContentKind -Kind $kinds.Variant -Generated @($emptyVariantRule) -Parts @(
            New-ContentPart -FileSets @($variantFiles)
            New-ContentPart -BuiltInIds @(Get-BuiltInVariantIds)
        )
        New-ContentKind -Kind $kinds.ShipSystem -Parts @(
            New-ContentPart -FileSets @($shipSystemFiles) `
                -Spreadsheets @(New-SpreadsheetSource -Path 'data/shipsystems/ship_systems.csv')
        )
        New-ContentKind -Kind $kinds.Projectile -Parts @(
            New-ContentPart -FileSets @($projectileFiles, $systemProjectileFiles)
        )

        # The campaign's economy and its people.
        New-ContentKind -Kind $kinds.MarketCondition -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/campaign/market_conditions.csv')
        )
        New-ContentKind -Kind $kinds.Industry -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/campaign/industries.csv')
        )
        New-ContentKind -Kind $kinds.SpecialItem -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/campaign/special_items.csv')
        )
        New-ContentKind -Kind $kinds.Commodity -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/campaign/commodities.csv')
        )
        New-ContentKind -Kind $kinds.Ability -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/campaign/abilities.csv')
        )
        New-ContentKind -Kind $kinds.Submarket -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/campaign/submarkets.csv')
        )
        New-ContentKind -Kind $kinds.Faction -Parts @(
            New-ContentPart -FileSets @($factionFiles)
        )
        New-ContentKind -Kind $kinds.Skill -Parts @(
            New-ContentPart -FileSets @($skillFiles) -Spreadsheets @($skillRows, $aptitudeEffectRows)
        )
        New-ContentKind -Kind $kinds.Aptitude -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/characters/skills/aptitude_data.csv')
        )
        New-ContentKind -Kind $kinds.Personality -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/characters/personalities.csv')
        )
        New-ContentKind -Kind $kinds.PersonMission -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path 'data/campaign/person_missions.csv' -KeyColumns @('mission id'))
        )
        New-ContentKind -Kind $kinds.BarEvent -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path 'data/campaign/bar_events.csv' -KeyColumns @('bar event id'))
        )

        # JSON files keyed by ID. Some skip a key whose value is not an object; the others fail on one.
        New-ContentKind -Kind $kinds.CustomEntity -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/config/custom_entities.json')
        )
        New-ContentKind -Kind $kinds.Planet -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/config/planets.json' -IsObjectEntryOnly)
        )
        New-ContentKind -Kind $kinds.Terrain -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/campaign/terrain.json' -IsObjectEntryOnly)
        )
        New-ContentKind -Kind $kinds.BattleObjective -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/config/battle_objectives.json')
        )
        New-ContentKind -Kind $kinds.EngineStyle -Parts @(
            New-ContentPart -KeyedFiles @(
                New-KeyedFileSource -Path 'data/config/engine_styles.json' -IsObjectEntryOnly)
        )
        New-ContentKind -Kind $kinds.HullStyle -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/config/hull_styles.json' -IsObjectEntryOnly)
        )
        New-ContentKind -Kind $kinds.CampaignEvent -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/campaign/events.json')
        )
        New-ContentKind -Kind $kinds.Ping -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/campaign/pings.json' -IsObjectEntryOnly)
        )
        New-ContentKind -Kind $kinds.IntelTag -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/config/tag_data.json')
        )
        New-ContentKind -Kind $kinds.ContactTag -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/config/contact_tag_data.json')
        )

        # sounds.json keys its sounds at the top level, and its music sets under "music".
        New-ContentKind -Kind $kinds.Sound -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/config/sounds.json' -ExcludedKeys @('music'))
        )
        New-ContentKind -Kind $kinds.MusicSet -Parts @(
            New-ContentPart -KeyedFiles @(New-KeyedFileSource -Path 'data/config/sounds.json' -NestedKey 'music')
        )

        # The game keys a description by its ID and type, joined by an underscore, and needs both.
        New-ContentKind -Kind $kinds.Description -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path 'data/strings/descriptions.csv' -KeyColumns @('id', 'type') `
                    -IdJoiner '_' -RequiresEveryKeyColumn)
        )

        # Sector generation tables. One keyed by several columns keys its specs by their values joined with _|_,
        # skipping a key opening with #.
        New-ContentKind -Kind $kinds.StarGenData -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/campaign/procgen/star_gen_data.csv')
        )
        New-ContentKind -Kind $kinds.AgeGenData -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/campaign/procgen/age_gen_data.csv')
        )
        New-ContentKind -Kind $kinds.ObjectiveGenData -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path 'data/campaign/procgen/objective_gen_data.csv')
        )
        New-ContentKind -Kind $kinds.LocationGenData -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/campaign/procgen/location_gen_data.csv')
        )
        New-ContentKind -Kind $kinds.NameGenData -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path 'data/campaign/procgen/name_gen_data.csv' -KeyColumns @('name', 'tags') `
                    -SkipsHashIds)
        )
        New-ContentKind -Kind $kinds.PlanetGenData -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path $planetGenPath -KeyColumns $planetGenKeyColumns -IdColumn 'id' `
                    -RowFilterColumn 'type' -RowFilterValue 'PLANET')
        )
        New-ContentKind -Kind $kinds.CategoryGenData -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path $planetGenPath -KeyColumns $planetGenKeyColumns -IdColumn 'category' `
                    -RowFilterColumn 'type' -RowFilterValue 'CATEGORY')
        )
        New-ContentKind -Kind $kinds.TerrainGenData -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path $planetGenPath -IdColumn 'id' -RowFilterColumn 'type' `
                    -RowFilterValue 'TERRAIN')
        )
        New-ContentKind -Kind $kinds.ConditionGenData -Parts @(
            New-ContentPart -Spreadsheets @(New-SpreadsheetSource -Path 'data/campaign/procgen/condition_gen_data.csv')
        )
        New-ContentKind -Kind $kinds.DropGroup -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path 'data/campaign/procgen/drop_groups.csv' `
                    -KeyColumns @('commodity', 'group') -SkipsHashIds)
        )
        New-ContentKind -Kind $kinds.SalvageEntityGenData -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path 'data/campaign/procgen/salvage_entity_gen_data.csv')
        )

        # Combat missions and dialogue rules, which the game keeps by ID outside its spec stores.
        # The game turns curly quotes straight before it reads rules.csv.
        New-ContentKind -Kind $kinds.Mission -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path 'data/missions/mission_list.csv' -KeyColumns @('mission') -SkipsHashIds)
        )
        New-ContentKind -Kind $kinds.Rule -Parts @(
            New-ContentPart -Spreadsheets @(
                New-SpreadsheetSource -Path 'data/campaign/rules.csv' -SkipsHashIds -ConvertsCurlyQuotes)
        )
    )
}
