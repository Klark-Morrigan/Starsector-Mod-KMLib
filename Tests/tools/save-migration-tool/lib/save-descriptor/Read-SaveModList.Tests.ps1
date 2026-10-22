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

        $mods = Read-SaveModList -SaveData $script:SaveData -ListName 'allModsEverEnabled' -IdIndex $script:IdIndex

        $mods.Id | Should -Be @('mod-z2', 'mod-z3')

        Should -Invoke ConvertFrom-EnabledModData -ParameterFilter { $IdIndex['marker'] -eq 'passed through' } -Times 2
    }

    It 'follows the list itself through a back-pointer' {

        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml('<SaveGameData><enabledMods ref="4"/></SaveGameData>')

        Mock Resolve-XStreamReference { $script:SaveData.SelectSingleNode('enabledMods') }

        (Read-SaveModList -SaveData $document.DocumentElement -ListName 'enabledMods' -IdIndex @{}).Id |
            Should -Be @('mod-z5')
    }

    It 'returns an empty list for a list with no entries' {

        $mods = Read-SaveModList -SaveData $script:SaveData -ListName 'emptyList' -IdIndex $script:IdIndex

        $null -eq $mods | Should -BeFalse
        $mods.Count | Should -Be 0
    }

    It 'returns null when the descriptor has no such list' {

        Read-SaveModList -SaveData $script:SaveData -ListName 'missingList' -IdIndex $script:IdIndex |
            Should -BeNullOrEmpty
    }
}
