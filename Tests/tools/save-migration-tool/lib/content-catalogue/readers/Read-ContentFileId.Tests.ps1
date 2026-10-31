BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content-catalogue\readers\Read-ContentFileId.ps1"
    . "$libRoot\data-file\ConvertFrom-StarsectorJson.ps1"
    . "$libRoot\data-file\Get-JsonPropertyValue.ps1"
    . "$libRoot\data-file\Read-StarsectorText.ps1"
    . "$libRoot\data-file\Remove-JsonComment.ps1"
}

Describe 'Read-ContentFileId' {

    BeforeEach {

        Mock Remove-JsonComment { $Text }
        Mock ConvertFrom-StarsectorJson { [ordered]@{ parsed = $Text } }

        # The parser hands back ordered hashtables, so the fake looks the key up in one.
        Mock Get-JsonPropertyValue {

            if ($Object.Contains($Name)) {
                $Object[$Name]
            }
        }
    }

    It 'takes a key written once with a plain string value without the full parse' {

        # ARRANGE
        Mock Read-StarsectorText { "{`n  `"hullId`" : `"alpha`",`n  `"hullName`": `"Alpha`"`n}" }

        $expectedId = 'alpha'

        # ACT
        $actualId = Read-ContentFileId -Path 'C:\mods\a\alpha.ship' -IdKey 'hullId'

        # ASSERT
        $actualId | Should -Be $expectedId
        Should -Invoke Read-StarsectorText -ParameterFilter { $Path -eq 'C:\mods\a\alpha.ship' } -Times 1 -Exactly
        Should -Invoke ConvertFrom-StarsectorJson -Times 0 -Exactly
    }

    It 'looks for the key in the text with its # comments stripped' {

        # ARRANGE
        Mock Read-StarsectorText { '{ "hullId": "alpha" } # "hullId": "commented"' }
        Mock Remove-JsonComment { '{ "hullId": "alpha" }' }

        $expectedId = 'alpha'

        # ACT
        $actualId = Read-ContentFileId -Path 'C:\a.ship' -IdKey 'hullId'

        # ASSERT
        $actualId | Should -Be $expectedId
    }

    # The game's own .faction files write id:"hegemony", which org.json takes.
    It 'takes the key written <Description> without the full parse' -ForEach @(
        @{ Description = 'bare'; Text = "{`n`tid:`"hegemony`",`n`tdisplayName:`"Hegemony`"`n}" }
        @{ Description = 'in single quotes'; Text = "{ 'id': `"hegemony`" }" }
        @{ Description = 'with = between it and its value'; Text = '{ "id" = "hegemony" }' }
        @{ Description = 'with => between it and its value'; Text = '{ id => "hegemony" }' }
    ) {

        # ARRANGE
        Mock Read-StarsectorText { $Text }

        $expectedId = 'hegemony'

        # ACT
        $actualId = Read-ContentFileId -Path 'C:\h.faction' -IdKey 'id'

        # ASSERT
        $actualId | Should -Be $expectedId
        Should -Invoke ConvertFrom-StarsectorJson -Times 0 -Exactly
    }

    It 'takes no bare key that is part of a longer name' {

        # ARRANGE
        Mock Read-StarsectorText { '{ proj_id: "x", id2: "y", "id": "kept" }' }

        $expectedId = 'kept'

        # ACT
        $actualId = Read-ContentFileId -Path 'C:\a.proj' -IdKey 'id'

        # ASSERT
        $actualId | Should -Be $expectedId
        Should -Invoke ConvertFrom-StarsectorJson -Times 0 -Exactly
    }

    It 'matches the key exactly, so a key that only contains it is not taken' {

        # ARRANGE
        Mock Read-StarsectorText { '{ "baseHullId": "base", "skinHullId": "skin" }' }

        $expectedId = 'skin'

        # ACT
        $actualId = Read-ContentFileId -Path 'C:\a.skin' -IdKey 'skinHullId'

        # ASSERT
        $actualId | Should -Be $expectedId
    }

    # A mod's copy that patches a few keys of another's copy sets no ID.
    It 'returns null without the full parse when the key never appears' {

        # ARRANGE
        Mock Read-StarsectorText { '{ "hullName": "Patched" }' }

        # ACT
        $actualId = Read-ContentFileId -Path 'C:\a.ship' -IdKey 'hullId'

        # ASSERT
        $actualId | Should -BeNullOrEmpty
        Should -Invoke ConvertFrom-StarsectorJson -Times 0 -Exactly
    }

    It 'falls back to the full parse for <Description>' -ForEach @(
        @{ Description = 'a key written twice'; Text = '{ "id": "a", "nested": { "id": "b" } }' }
        @{ Description = 'a value holding an escape'; Text = '{ "id": "a\"b" }' }
        @{ Description = 'a value that is not a string'; Text = '{ "id": 7 }' }
    ) {

        # ARRANGE
        Mock Read-StarsectorText { $Text }
        Mock ConvertFrom-StarsectorJson { [ordered]@{ id = 'parsed' } }

        $expectedId = 'parsed'

        # ACT
        $actualId = Read-ContentFileId -Path 'C:\a.wpn' -IdKey 'id'

        # ASSERT
        $actualId | Should -Be $expectedId
        Should -Invoke ConvertFrom-StarsectorJson -Times 1 -Exactly
    }

    # The full parse refuses a byte order mark, as the game does, where the pattern would read straight past it.
    It 'hands a text opening with a byte order mark to the full parse' {

        # ARRANGE
        Mock Read-StarsectorText { "$([char] 0xFEFF){ `"id`": `"a`" }" }
        Mock ConvertFrom-StarsectorJson { throw 'starts with a byte order mark' }

        $expectedMessage = 'starts with a byte order mark'

        # ACT
        $actualError = { Read-ContentFileId -Path 'C:\a.wpn' -IdKey 'id' } | Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -Be $expectedMessage
    }

    It 'returns null when the full parse finds no key at the top level' {

        # ARRANGE
        Mock Read-StarsectorText { '{ "nested": { "id": "a" }, "other": { "id": "b" } }' }
        Mock ConvertFrom-StarsectorJson { [ordered]@{ nested = 'x' } }

        # ACT
        $actualId = Read-ContentFileId -Path 'C:\a.wpn' -IdKey 'id'

        # ASSERT
        $actualId | Should -BeNullOrEmpty
    }

    It 'fails when the full parse finds a key that is not text' {

        # ARRANGE
        Mock Read-StarsectorText { '{ "id": 7 }' }
        Mock ConvertFrom-StarsectorJson { [ordered]@{ id = 7 } }

        $expectedMessage = 'its id is not text'

        # ACT
        $actualError = { Read-ContentFileId -Path 'C:\a.wpn' -IdKey 'id' } | Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -Be $expectedMessage
    }
}
