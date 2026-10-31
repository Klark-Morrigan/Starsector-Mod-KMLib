BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\Resolve-XStreamReference.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-descriptor\ConvertFrom-EnabledModData.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-descriptor\Read-SaveModList.ps1"
}

Describe 'Read-SaveModList' {

    BeforeEach {

        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml(
            '<SaveGameData>' +
            '<allModsEverEnabled z="1"><EnabledModData z="2"/><EnabledModData z="3"/></allModsEverEnabled>' +
            '<enabledMods z="4"><EnabledModData z="5"/></enabledMods>' +
            '<emptyList z="6"></emptyList>' +
            '</SaveGameData>')

        $script:SaveData = $document.DocumentElement
        $script:IdIndex = @{ 'marker' = 'passed through' }

        Mock Resolve-XStreamReference { $Element }
        Mock ConvertFrom-EnabledModData { [pscustomobject]@{ Id = "mod-z$($EnabledModData.GetAttribute('z'))" } }
    }

    It 'reads each entry of the named list, in order, passing the index on' {

        # ARRANGE
        $listName = 'allModsEverEnabled'

        $expectedIds = @('mod-z2', 'mod-z3')

        # ACT
        $actualMods = Read-SaveModList -SaveData $script:SaveData -ListName $listName -IdIndex $script:IdIndex

        # ASSERT
        $actualMods.Id | Should -Be $expectedIds
        Should -Invoke ConvertFrom-EnabledModData -ParameterFilter { $IdIndex['marker'] -eq 'passed through' } -Times 2
    }

    It 'follows the list itself through a back-pointer' {

        # ARRANGE
        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml('<SaveGameData><enabledMods ref="4"/></SaveGameData>')

        Mock Resolve-XStreamReference { $script:SaveData.SelectSingleNode('enabledMods') }

        $expectedIds = @('mod-z5')

        # ACT
        $actualMods = Read-SaveModList -SaveData $document.DocumentElement -ListName 'enabledMods' -IdIndex @{}

        # ASSERT
        $actualMods.Id | Should -Be $expectedIds
    }

    It 'returns an empty list for a list with no entries' {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualMods = Read-SaveModList -SaveData $script:SaveData -ListName 'emptyList' -IdIndex $script:IdIndex

        # ASSERT
        $null -eq $actualMods | Should -BeFalse
        $actualMods.Count | Should -Be $expectedCount
    }

    It 'returns null when the descriptor has no such list' {

        # ACT
        $actualMods = Read-SaveModList -SaveData $script:SaveData -ListName 'missingList' -IdIndex $script:IdIndex

        # ASSERT
        $actualMods | Should -BeNullOrEmpty
    }
}
