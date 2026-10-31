BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content\Get-ContentKindNames.ps1"
}

Describe 'Get-ContentKindNames' {

    It 'names each kind of content after itself' {

        # ARRANGE
        $expectedMisnamedCount = 0

        # ACT
        $actualKinds = Get-ContentKindNames

        # ASSERT
        @($actualKinds.PSObject.Properties | Where-Object { $_.Name -cne $_.Value }).Count |
            Should -Be $expectedMisnamedCount
    }

    It 'names the kinds of content the game defines by ID, and the two a failed load can name besides' {

        # ARRANGE
        $expectedNames = @(
            'Hull', 'Weapon', 'FighterWing', 'Hullmod', 'Variant', 'ShipSystem', 'Projectile'
            'MarketCondition', 'Industry', 'SpecialItem', 'Commodity', 'Ability', 'Submarket', 'Faction', 'Skill'
            'Aptitude', 'Personality', 'CustomEntity', 'Planet', 'Terrain', 'BattleObjective', 'EngineStyle'
            'HullStyle', 'PersonMission', 'BarEvent', 'CampaignEvent', 'Ping', 'IntelTag', 'ContactTag', 'Sound'
            'MusicSet', 'Description', 'StarGenData', 'AgeGenData', 'ObjectiveGenData', 'LocationGenData'
            'NameGenData', 'PlanetGenData', 'CategoryGenData', 'TerrainGenData', 'ConditionGenData', 'DropGroup'
            'SalvageEntityGenData', 'Mission', 'Rule', 'Spec', 'Class'
        )

        # ACT
        $actualKinds = Get-ContentKindNames

        # ASSERT
        @($actualKinds.PSObject.Properties.Name) | Should -Be $expectedNames
    }
}
