BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\save-descriptor\Format-DroppedModSummary.ps1"

    function script:New-Mods {
        # Mods as Read-SaveModList reads them, by ID alone.
        param([string[]] $Ids)

        return @($Ids | ForEach-Object { [pscustomobject]@{ Id = $_ } })
    }
}

Describe 'Format-DroppedModSummary' {

    BeforeEach {

        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ConsoleNumber { "<Number>$Number" }
    }

    It 'counts and names the mods enabled once and not at the last save' {

        $summary = Format-DroppedModSummary `
            -SavedMods (New-Mods 'alpha') `
            -EverEnabledMods (New-Mods 'alpha', 'retired', 'gone')

        $summary | Should -Be '<Number>2 more were enabled in an earlier save and are not now: <Mod>retired, <Mod>gone'
    }

    # A save's mod IDs match case-sensitively, as the game matches them.
    It 'reads an ID in another case as another mod' {

        Format-DroppedModSummary -SavedMods (New-Mods 'alpha') -EverEnabledMods (New-Mods 'Alpha') |
            Should -Be '<Number>1 more were enabled in an earlier save and are not now: <Mod>Alpha'
    }

    It 'counts none, naming none, when every mod is still enabled or no list was written' {

        Format-DroppedModSummary -SavedMods (New-Mods 'alpha') -EverEnabledMods $null |
            Should -Be '<Number>0 more were enabled in an earlier save and are not now'
    }
}
