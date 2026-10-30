BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleList.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\mod-drift\Format-DroppedModSummary.ps1"

    function script:New-Mods {
        # Mods as Read-SaveModList reads them, by ID alone.
        param([string[]] $Ids)

        return @($Ids | ForEach-Object { [pscustomobject]@{ Id = $_ } })
    }
}

Describe 'Format-DroppedModSummary' {

    BeforeEach {

        Mock Format-ConsoleList { "<$Role>$($Values -join '+')" }
        Mock Format-ConsoleNumber { "<Number>$Number" }
    }

    It 'counts and names the mods enabled once and not at the last save' {

        # ARRANGE
        $savedMods = New-Mods 'alpha'
        $everEnabledMods = New-Mods 'alpha', 'retired', 'gone'
        $expectedSummary = '<Number>2 more were enabled in an earlier save and are not now: <Mod>retired+gone'

        # ACT
        $actualSummary = Format-DroppedModSummary -SavedMods $savedMods -EverEnabledMods $everEnabledMods

        # ASSERT
        $actualSummary | Should -Be $expectedSummary
    }

    # A save's mod IDs match case-sensitively, as the game matches them.
    It 'reads an ID in another case as another mod' {

        # ARRANGE
        $savedMods = New-Mods 'alpha'
        $everEnabledMods = New-Mods 'Alpha'
        $expectedSummary = '<Number>1 more were enabled in an earlier save and are not now: <Mod>Alpha'

        # ACT
        $actualSummary = Format-DroppedModSummary -SavedMods $savedMods -EverEnabledMods $everEnabledMods

        # ASSERT
        $actualSummary | Should -Be $expectedSummary
    }

    It 'counts none, naming none, when every mod is still enabled or no list was written' {

        # ARRANGE
        $savedMods = New-Mods 'alpha'
        $expectedSummary = '<Number>0 more were enabled in an earlier save and are not now'

        # ACT
        $actualSummary = Format-DroppedModSummary -SavedMods $savedMods -EverEnabledMods $null

        # ASSERT
        $actualSummary | Should -Be $expectedSummary
        Should -Invoke Format-ConsoleList -Times 0
    }
}
