# Where the game's own classes write content IDs into a campaign.xml, as of 0.98a, per kind.
# XStream names an element after its field, or after the class's short alias, and the game aliases its classes:
# FMmbr is a fleet member, CIStack a cargo stack, SpID a special item, MCon a market condition, Plnt a planet.
# A mod's own classes write IDs in fields of their own choosing, which no table can know, so the sites are the game's.
#
# An ID is counted once for every time a site names it.
function Get-SaveReferenceSites {

    $kinds = Get-ContentKindNames

    # A special item's data names the content it unlocks or installs, for the game's own items.
    $blueprintKindByItem = [ordered]@{
        ship_bp = $kinds.Hull
        weapon_bp = $kinds.Weapon
        fighter_bp = $kinds.FighterWing
        modspec = $kinds.Hullmod
        industry_bp = $kinds.Industry
    }

    # Lists of IDs, each written as one <st> per ID: the factions' known and priority content, and a variant's loadout.
    $listKindByElement = [ordered]@{
        knownShips = $kinds.Hull
        priorityShips = $kinds.Hull
        shipsWhenImporting = $kinds.Hull
        overriddenHulls = $kinds.Hull
        knownWeapons = $kinds.Weapon
        priorityWeapons = $kinds.Weapon
        wng = $kinds.FighterWing
        knownFighters = $kinds.FighterWing
        priorityFighters = $kinds.FighterWing
        hM = $kinds.Hullmod
        pM = $kinds.Hullmod
        sMods = $kinds.Hullmod
        sModdedBuiltIns = $kinds.Hullmod
        suM = $kinds.Hullmod
        knownHullMods = $kinds.Hullmod
        priorityHullMods = $kinds.Hullmod
        knownIndustries = $kinds.Industry
        cond = $kinds.MarketCondition
        surveyed = $kinds.MarketCondition
        suppressedConditions = $kinds.MarketCondition
        illegal = $kinds.Commodity
    }

    # A cargo stack names its content in its first child, by the stack's type.
    $stackKindByType = [ordered]@{
        WEAPONS = $kinds.Weapon
        FIGHTER_CHIP = $kinds.FighterWing
        RESOURCES = $kinds.Commodity
    }

    $sites = [System.Collections.Generic.List[object]]::new()

    # A variant names its hull, whether a fleet member's saved variant or one kept elsewhere.
    foreach ($variantElement in
            'savedVariant',
            'HullVariantSpec') {

        $sites.Add((New-SaveReferenceSite `
            -Kind $kinds.Hull `
            -Path @($variantElement) `
            -Attribute 'hId'))
    }

    foreach ($stackType in $stackKindByType.Keys) {

        $sites.Add((New-SaveReferenceSite `
            -Kind $stackKindByType[$stackType] `
            -Path @('CIStack', 'd') `
            -FilterAttribute 't' `
            -FilterValues @($stackType)))
    }

    # A variant's weapons map each slot to a weapon: the second <st> of each entry.
    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Weapon `
        -Path @('wpn', 'e', 'st') `
        -Position 2))

    foreach ($listElement in $listKindByElement.Keys) {

        $sites.Add((New-SaveReferenceSite `
            -Kind $listKindByElement[$listElement] `
            -Path @($listElement, 'st')))
    }

    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.SpecialItem `
        -Path @('SpID') `
        -Attribute 'i'))

    # An industry's installed item is a special item too.
    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.SpecialItem `
        -Path @('special') `
        -Attribute 'i'))

    foreach ($item in $blueprintKindByItem.Keys) {

        $sites.Add((New-SaveReferenceSite `
            -Kind $blueprintKindByItem[$item] `
            -Path @('SpID') `
            -Attribute 'd' `
            -FilterAttribute 'i' `
            -FilterValues @($item)))
    }

    # A market's industries are written under the class of each, which varies, with the ID as an attribute.
    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Industry `
        -Path @('industries', '*') `
        -Attribute 'id'))

    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.MarketCondition `
        -Path @('MCon') `
        -Attribute 'i'))

    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Submarket `
        -Path @('Submarket') `
        -Attribute 's'))

    # A person, under whatever field holds one, carries its faction and personality as attributes,
    # and its skills as a JSON object of skill levels.
    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Faction `
        -Path @('*') `
        -Attribute 'fid'))

    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Personality `
        -Path @('*') `
        -Attribute 'pid'))

    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Skill `
        -Path @('stats', 's') `
        -AreJsonKeysIds))

    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Skill `
        -Path @('OSGRec') `
        -Attribute 's'))

    # The faction manager's relations are left out: a mod seeds relations with factions that may never have been
    # installed, so a faction they name says nothing of what the save uses.
    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Faction `
        -Path @('Faction', 'id')))

    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Faction `
        -Path @('Market', 'factionId')))

    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Ability `
        -Path @('AbilitySlot', 'abilityId')))

    # A custom entity packs width, height, radius, sheet cell X and Y, and type into one JSON object,
    # each under f and its place in that list, so the type is f5.
    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.CustomEntity `
        -Path @('CCEnt', 'j1') `
        -JsonKey 'f5'))

    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Planet `
        -Path @('Plnt', 'type')))

    $sites.Add((New-SaveReferenceSite `
        -Kind $kinds.Terrain `
        -Path @('CampaignTerrain') `
        -Attribute 'type'))

    return $sites.ToArray()
}
