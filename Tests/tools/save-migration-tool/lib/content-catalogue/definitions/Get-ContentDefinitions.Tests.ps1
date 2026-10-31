BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content\Get-ContentKindNames.ps1"

    foreach ($name in @(
            'Get-BuiltInVariantIds'
            'Get-ContentDefinitions'
            'New-ContentFileSet'
            'New-ContentKind'
            'New-ContentPart'
            'New-KeyedFileSource'
            'New-SpreadsheetSource'
        )) {

        . "$libRoot\content-catalogue\definitions\$name.ps1"
    }

    function script:Format-Spreadsheet {
        # Writes a spreadsheet source as path[key columns], then each way it departs from a plain id key.
        param([object] $Spreadsheet)

        $text = "$($Spreadsheet.Path)[$($Spreadsheet.KeyColumns -join ',')]"

        if ($Spreadsheet.IdColumn) {
            $text += " id=$($Spreadsheet.IdColumn)"
        }

        if ($Spreadsheet.KeyColumns.Count -gt 1 -and -not $Spreadsheet.IdColumn) {
            $text += " join=$($Spreadsheet.IdJoiner)"
        }

        if ($Spreadsheet.RowFilterColumn) {
            $text += " where $($Spreadsheet.RowFilterColumn)=$($Spreadsheet.RowFilterValue)"
        }

        foreach ($flag in 'SkipsHashIds', 'RequiresEveryKeyColumn', 'ConvertsCurlyQuotes') {

            if ($Spreadsheet.$flag) {
                $text += " $flag"
            }
        }

        return $text
    }

    function script:Format-Kind {
        # Writes a kind as one line: each part's sources joined by +, parts by |, then the IDs the game builds.
        param([object] $Definition)

        $parts = foreach ($part in $Definition.Parts) {

            $sources = @(
                foreach ($fileSet in $part.FileSets) {

                    if ($fileSet.ListedBy) {
                        "files listed by $(Format-Spreadsheet $fileSet.ListedBy) by $($fileSet.IdKey)"
                    } else {

                        $subfolderMark = if ($fileSet.HasSubfolders) {
                            '>'
                        } else {
                            ''
                        }

                        "$($fileSet.Folder)$subfolderMark/*$($fileSet.Ending) by $($fileSet.IdKey)"
                    }
                }

                foreach ($spreadsheet in $part.Spreadsheets) {
                    Format-Spreadsheet $spreadsheet
                }

                foreach ($keyedFile in $part.KeyedFiles) {

                    $text = "$($keyedFile.Path){$($keyedFile.NestedKey)}"

                    if ($keyedFile.ExcludedKeys.Count -gt 0) {
                        $text += " without $($keyedFile.ExcludedKeys -join ',')"
                    }

                    if ($keyedFile.IsObjectEntryOnly) {
                        $text += ' objects only'
                    }

                    $text
                }

                if ($part.BuiltInIds.Count -gt 0) {
                    "built in: $($part.BuiltInIds -join ',')"
                }
            )

            $sources -join ' + '
        }

        $line = "$($Definition.Kind): $($parts -join ' | ')"

        foreach ($rule in $Definition.Generated) {

            $line += "; builds $($rule.FromKind)+$($rule.Suffix)"

            if ($rule.IsSuffixedBaseSkipped) {
                $line += ' unless suffixed'
            }
        }

        return $line
    }
}

Describe 'Get-ContentDefinitions' {

    BeforeEach {
        Mock Get-BuiltInVariantIds { 'omen1', 'condor1' }
    }

    It 'defines every kind the game keeps by ID, from the files it loads at startup' {

        # ARRANGE
        $expectedKinds = @(
            ('Hull: data/hulls/*ship by hullId + data/hulls/ship_data.csv[id] | ' +
                'data/hulls/skins>/*skin by skinHullId; builds Hull+_default_D unless suffixed')
            ('Weapon: data/weapons/*wpn by id + data/shipsystems/wpn/*wpn by id + ' +
                'data/weapons/weapon_data.csv[id]')
            'FighterWing: data/hulls/wing_data.csv[id]'
            'Hullmod: data/hullmods/hull_mods.csv[id]'
            'Variant: data/variants>/*variant by variantId | built in: omen1,condor1; builds Hull+_Hull'
            'ShipSystem: data/shipsystems/*system by id + data/shipsystems/ship_systems.csv[id]'
            'Projectile: data/weapons/proj/*proj by id + data/shipsystems/proj/*proj by id'
            'MarketCondition: data/campaign/market_conditions.csv[id]'
            'Industry: data/campaign/industries.csv[id]'
            'SpecialItem: data/campaign/special_items.csv[id]'
            'Commodity: data/campaign/commodities.csv[id]'
            'Ability: data/campaign/abilities.csv[id]'
            'Submarket: data/campaign/submarkets.csv[id]'
            'Faction: files listed by data/world/factions/factions.csv[faction] by id'
            ('Skill: data/characters/skills/*skill by id + data/characters/skills/skill_data.csv[id] + ' +
                'data/characters/skills/aptitude_data.csv[id] id=effect_skill_id')
            'Aptitude: data/characters/skills/aptitude_data.csv[id]'
            'Personality: data/characters/personalities.csv[id]'
            'PersonMission: data/campaign/person_missions.csv[mission id]'
            'BarEvent: data/campaign/bar_events.csv[bar event id]'
            'CustomEntity: data/config/custom_entities.json{}'
            'Planet: data/config/planets.json{} objects only'
            'Terrain: data/campaign/terrain.json{} objects only'
            'BattleObjective: data/config/battle_objectives.json{}'
            'EngineStyle: data/config/engine_styles.json{} objects only'
            'HullStyle: data/config/hull_styles.json{} objects only'
            'CampaignEvent: data/campaign/events.json{}'
            'Ping: data/campaign/pings.json{} objects only'
            'IntelTag: data/config/tag_data.json{}'
            'ContactTag: data/config/contact_tag_data.json{}'
            'Sound: data/config/sounds.json{} without music'
            'MusicSet: data/config/sounds.json{music}'
            'Description: data/strings/descriptions.csv[id,type] join=_ RequiresEveryKeyColumn'
            'StarGenData: data/campaign/procgen/star_gen_data.csv[id]'
            'AgeGenData: data/campaign/procgen/age_gen_data.csv[id]'
            'ObjectiveGenData: data/campaign/procgen/objective_gen_data.csv[id]'
            'LocationGenData: data/campaign/procgen/location_gen_data.csv[id]'
            'NameGenData: data/campaign/procgen/name_gen_data.csv[name,tags] join=_|_ SkipsHashIds'
            'PlanetGenData: data/campaign/procgen/planet_gen_data.csv[id,type,category] id=id where type=PLANET'
            ('CategoryGenData: data/campaign/procgen/planet_gen_data.csv[id,type,category] id=category ' +
                'where type=CATEGORY')
            'TerrainGenData: data/campaign/procgen/planet_gen_data.csv[id] id=id where type=TERRAIN'
            'ConditionGenData: data/campaign/procgen/condition_gen_data.csv[id]'
            'DropGroup: data/campaign/procgen/drop_groups.csv[commodity,group] join=_|_ SkipsHashIds'
            'SalvageEntityGenData: data/campaign/procgen/salvage_entity_gen_data.csv[id]'
            'Mission: data/missions/mission_list.csv[mission] SkipsHashIds'
            'Rule: data/campaign/rules.csv[id] SkipsHashIds ConvertsCurlyQuotes'
        )

        # ACT
        $actualDefinitions = Get-ContentDefinitions

        # ASSERT
        @($actualDefinitions | ForEach-Object { Format-Kind $_ }) | Should -Be $expectedKinds
    }
}
