BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content\Get-ContentKindNames.ps1"
    . "$libRoot\save-references\Get-SaveReferenceSites.ps1"
    . "$libRoot\save-references\New-SaveReferenceSite.ps1"
}

Describe 'Get-SaveReferenceSites' {

    It 'lists where the game''s own classes write each kind''s IDs, by kind and site name' {

        # ARRANGE
        $expectedSites = @(
            'Hull savedVariant@hId'
            'Hull HullVariantSpec@hId'
            'Weapon CIStack[t=WEAPONS]/d'
            'FighterWing CIStack[t=FIGHTER_CHIP]/d'
            'Commodity CIStack[t=RESOURCES]/d'
            'Weapon wpn/e/st[2]'
            'Hull knownShips/st'
            'Hull priorityShips/st'
            'Hull shipsWhenImporting/st'
            'Hull overriddenHulls/st'
            'Weapon knownWeapons/st'
            'Weapon priorityWeapons/st'
            'FighterWing wng/st'
            'FighterWing knownFighters/st'
            'FighterWing priorityFighters/st'
            'Hullmod hM/st'
            'Hullmod pM/st'
            'Hullmod sMods/st'
            'Hullmod sModdedBuiltIns/st'
            'Hullmod suM/st'
            'Hullmod knownHullMods/st'
            'Hullmod priorityHullMods/st'
            'Industry knownIndustries/st'
            'MarketCondition cond/st'
            'MarketCondition surveyed/st'
            'MarketCondition suppressedConditions/st'
            'Commodity illegal/st'
            'SpecialItem SpID@i'
            'SpecialItem special@i'
            'Hull SpID[i=ship_bp]@d'
            'Weapon SpID[i=weapon_bp]@d'
            'FighterWing SpID[i=fighter_bp]@d'
            'Hullmod SpID[i=modspec]@d'
            'Industry SpID[i=industry_bp]@d'
            'Industry industries/*@id'
            'MarketCondition MCon@i'
            'Submarket Submarket@s'
            'Faction *@fid'
            'Personality *@pid'
            'Skill stats/s{keys}'
            'Skill OSGRec@s'
            'Faction Faction/id'
            'Faction Market/factionId'
            'Ability AbilitySlot/abilityId'
            'CustomEntity CCEnt/j1{f5}'
            'Planet Plnt/type'
            'Terrain CampaignTerrain@type'
        )

        # ACT
        $actualSites = @(Get-SaveReferenceSites |
            ForEach-Object { "$($_.Kind) $($_.Name)" })

        # ASSERT
        $actualSites | Should -Be $expectedSites
    }
}
