BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content-catalogue\Read-ContentFileId.ps1"
    . "$libRoot\mod-info\ConvertFrom-StarsectorJson.ps1"
    . "$libRoot\mod-info\Get-JsonPropertyValue.ps1"
    . "$libRoot\mod-info\Read-StarsectorText.ps1"
    . "$libRoot\mod-info\Remove-JsonComment.ps1"
}

Describe 'Read-ContentFileId' {

    BeforeEach {

        Mock Remove-JsonComment { $Text }
        Mock ConvertFrom-StarsectorJson { [pscustomobject]@{ parsed = $Text } }

        # Strict mode throws on a missing property, so the fake looks the name up first.
        Mock Get-JsonPropertyValue {

            $property = $Object.PSObject.Properties[$Name]

            if ($null -ne $property) {
                $property.Value
            }
        }
    }

    It 'takes a key written once with a plain string value without the full parse' {

        Mock Read-StarsectorText { "{`n  `"hullId`" : `"alpha`",`n  `"hullName`": `"Alpha`"`n}" }

        Read-ContentFileId -Path 'C:\mods\a\alpha.ship' -IdKey 'hullId' | Should -Be 'alpha'

        Should -Invoke Read-StarsectorText -ParameterFilter { $Path -eq 'C:\mods\a\alpha.ship' } -Times 1 -Exactly
        Should -Invoke ConvertFrom-StarsectorJson -Times 0 -Exactly
    }

    It 'looks for the key in the text with its # comments stripped' {

        Mock Read-StarsectorText { '{ "hullId": "alpha" } # "hullId": "commented"' }
        Mock Remove-JsonComment { '{ "hullId": "alpha" }' }

        Read-ContentFileId -Path 'C:\a.ship' -IdKey 'hullId' | Should -Be 'alpha'
    }

    # The game's own .faction files write id:"hegemony", which org.json takes.
    It 'takes the key written <Description> without the full parse' -ForEach @(
        @{ Description = 'bare'; Text = "{`n`tid:`"hegemony`",`n`tdisplayName:`"Hegemony`"`n}" }
        @{ Description = 'in single quotes'; Text = "{ 'id': `"hegemony`" }" }
        @{ Description = 'with = between it and its value'; Text = '{ "id" = "hegemony" }' }
        @{ Description = 'with => between it and its value'; Text = '{ id => "hegemony" }' }
    ) {

        Mock Read-StarsectorText { $Text }

        Read-ContentFileId -Path 'C:\h.faction' -IdKey 'id' | Should -Be 'hegemony'

        Should -Invoke ConvertFrom-StarsectorJson -Times 0 -Exactly
    }

    It 'takes no bare key that is part of a longer name' {

        Mock Read-StarsectorText { '{ proj_id: "x", id2: "y", "id": "kept" }' }

        Read-ContentFileId -Path 'C:\a.proj' -IdKey 'id' | Should -Be 'kept'

        Should -Invoke ConvertFrom-StarsectorJson -Times 0 -Exactly
    }

    It 'matches the key exactly, so a key that only contains it is not taken' {

        Mock Read-StarsectorText { '{ "baseHullId": "base", "skinHullId": "skin" }' }

        Read-ContentFileId -Path 'C:\a.skin' -IdKey 'skinHullId' | Should -Be 'skin'
    }

    # A mod's copy that patches a few keys of another's copy sets no ID.
    It 'returns null without the full parse when the key never appears' {

        Mock Read-StarsectorText { '{ "hullName": "Patched" }' }

        Read-ContentFileId -Path 'C:\a.ship' -IdKey 'hullId' | Should -BeNullOrEmpty

        Should -Invoke ConvertFrom-StarsectorJson -Times 0 -Exactly
    }

    It 'falls back to the full parse for <Description>' -ForEach @(
        @{ Description = 'a key written twice'; Text = '{ "id": "a", "nested": { "id": "b" } }' }
        @{ Description = 'a value holding an escape'; Text = '{ "id": "a\"b" }' }
        @{ Description = 'a value that is not a string'; Text = '{ "id": 7 }' }
    ) {

        Mock Read-StarsectorText { $Text }
        Mock ConvertFrom-StarsectorJson { [pscustomobject]@{ id = 'parsed' } }

        Read-ContentFileId -Path 'C:\a.wpn' -IdKey 'id' | Should -Be 'parsed'

        # Keys that differ only in case are two keys to the game, and fail the parse outside a hashtable.
        Should -Invoke ConvertFrom-StarsectorJson -ParameterFilter { $AsHashtable } -Times 1 -Exactly
    }

    # The full parse refuses a byte order mark, as the game does, where the pattern would read straight past it.
    It 'hands a text opening with a byte order mark to the full parse' {

        Mock Read-StarsectorText { "$([char] 0xFEFF){ `"id`": `"a`" }" }
        Mock ConvertFrom-StarsectorJson { throw 'starts with a byte order mark' }

        { Read-ContentFileId -Path 'C:\a.wpn' -IdKey 'id' } | Should -Throw 'starts with a byte order mark'
    }

    It 'returns null when the full parse finds no key at the top level' {

        Mock Read-StarsectorText { '{ "nested": { "id": "a" }, "other": { "id": "b" } }' }
        Mock ConvertFrom-StarsectorJson { [pscustomobject]@{ nested = 'x' } }

        Read-ContentFileId -Path 'C:\a.wpn' -IdKey 'id' | Should -BeNullOrEmpty
    }

    It 'fails when the full parse finds a key that is not text' {

        Mock Read-StarsectorText { '{ "id": 7 }' }
        Mock ConvertFrom-StarsectorJson { [pscustomobject]@{ id = 7 } }

        { Read-ContentFileId -Path 'C:\a.wpn' -IdKey 'id' } | Should -Throw 'its id is not text'
    }
}
