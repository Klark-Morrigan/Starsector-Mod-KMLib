# The kinds of content the game defines by ID, as its spec stores split them.
# Named once, so a failed load's missing content and the catalogue of defined content agree on each kind.
function Get-ContentKindNames {

    return [pscustomobject]@{
        # Ships and what they mount, each in a store of its own.
        Hull                 = 'Hull'
        Weapon               = 'Weapon'
        FighterWing          = 'FighterWing'
        Hullmod              = 'Hullmod'
        Variant              = 'Variant'
        ShipSystem           = 'ShipSystem'
        Projectile           = 'Projectile'

        # Spec types in the one generic store.
        MarketCondition      = 'MarketCondition'
        Industry             = 'Industry'
        SpecialItem          = 'SpecialItem'
        Commodity            = 'Commodity'
        Ability              = 'Ability'
        Submarket            = 'Submarket'
        Faction              = 'Faction'
        Skill                = 'Skill'
        Aptitude             = 'Aptitude'
        Personality          = 'Personality'
        CustomEntity         = 'CustomEntity'
        Planet               = 'Planet'
        Terrain              = 'Terrain'
        BattleObjective      = 'BattleObjective'
        EngineStyle          = 'EngineStyle'
        HullStyle            = 'HullStyle'
        PersonMission        = 'PersonMission'
        BarEvent             = 'BarEvent'
        CampaignEvent        = 'CampaignEvent'
        Ping                 = 'Ping'
        IntelTag             = 'IntelTag'
        ContactTag           = 'ContactTag'
        Sound                = 'Sound'
        MusicSet             = 'MusicSet'
        Description          = 'Description'
        StarGenData          = 'StarGenData'
        AgeGenData           = 'AgeGenData'
        ObjectiveGenData     = 'ObjectiveGenData'
        LocationGenData      = 'LocationGenData'
        NameGenData          = 'NameGenData'
        PlanetGenData        = 'PlanetGenData'
        CategoryGenData      = 'CategoryGenData'
        TerrainGenData       = 'TerrainGenData'
        ConditionGenData     = 'ConditionGenData'
        DropGroup            = 'DropGroup'
        SalvageEntityGenData = 'SalvageEntityGenData'

        # Content the game keeps by ID outside the spec stores.
        Mission              = 'Mission'
        Rule                 = 'Rule'

        # A missing ID of any generic store type, when its error names only the spec's class, often obfuscated.
        Spec                 = 'Spec'

        # A class a save names, which no loaded JAR holds.
        Class                = 'Class'
    }
}
