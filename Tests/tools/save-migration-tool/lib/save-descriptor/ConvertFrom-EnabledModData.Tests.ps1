BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Add-ModDefaultDisplay.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\Get-XmlChildText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\Resolve-XStreamReference.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-descriptor\ConvertFrom-SaveModVersion.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-descriptor\ConvertFrom-SaveVersionInfo.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-descriptor\ConvertFrom-EnabledModData.ps1"
}

Describe 'ConvertFrom-EnabledModData' {

    BeforeEach {

        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml(
            '<root>' +
            '<EnabledModData z="1">' +
            '<spec z="2"><id>alpha</id><name>Alpha</name><versionInfo z="3"/></spec>' +
            '</EnabledModData>' +
            '<EnabledModData z="4"><spec ref="2"/></EnabledModData>' +
            '<EnabledModData z="5"><spec z="6"><id>beta</id><versionInfo ref="3"/></spec></EnabledModData>' +
            '<EnabledModData z="7"><spec z="8"><id>gamma</id></spec></EnabledModData>' +
            '<EnabledModData z="9"/>' +
            '</root>')

        $script:Entries = $document.DocumentElement.SelectNodes('EnabledModData')
        $script:IdIndex = @{ 'marker' = 'passed through' }

        # Follows a ref through the document itself, so each case states its back-pointers as XML.
        Mock Resolve-XStreamReference {

            $id = $Element.GetAttribute('ref')
            if ($id) { $Element.OwnerDocument.SelectSingleNode("//*[@z='$id']") } else { $Element }
        }

        Mock Get-XmlChildText {

            $child = $Element.SelectSingleNode($Name)
            if ($null -ne $child -and $child.InnerText) { $child.InnerText }
        }

        Mock ConvertFrom-SaveModVersion {

            if ($VersionInfo) {
                "version-z$($VersionInfo.GetAttribute('z'))"
            } else {
                ''
            }
        }

        Mock ConvertFrom-SaveVersionInfo {

            if ($VersionInfo) {
                "parts-z$($VersionInfo.GetAttribute('z'))"
            } else {
                'no parts'
            }
        }

        Mock Add-ModDefaultDisplay {
            $Mod | Add-Member -NotePropertyName 'Display' -NotePropertyValue 'added' -PassThru
        }
    }

    It 'reads the ID, name, version and version parts of a spec written in place' {

        $mod = ConvertFrom-EnabledModData -EnabledModData $script:Entries[0] -IdIndex $script:IdIndex

        $mod.Id | Should -Be 'alpha'
        $mod.Name | Should -Be 'Alpha'
        $mod.Version | Should -Be 'version-z3'
        $mod.VersionInfo | Should -Be 'parts-z3'
    }

    It 'gives the mod the display every mod list shares' {
        (ConvertFrom-EnabledModData -EnabledModData $script:Entries[0] -IdIndex $script:IdIndex).Display |
            Should -Be 'added'
    }

    It 'follows a spec back-pointer, passing the index on' {

        $mod = ConvertFrom-EnabledModData -EnabledModData $script:Entries[1] -IdIndex $script:IdIndex

        $mod.Id | Should -Be 'alpha'
        Should -Invoke Resolve-XStreamReference -ParameterFilter { $IdIndex['marker'] -eq 'passed through' } -Times 3
    }

    It 'follows a versionInfo back-pointer' {

        $mod = ConvertFrom-EnabledModData -EnabledModData $script:Entries[2] -IdIndex $script:IdIndex

        $mod.Version | Should -Be 'version-z3'
        $mod.VersionInfo | Should -Be 'parts-z3'
    }

    It 'reads a spec with no versionInfo, and no name, as empty' {

        $mod = ConvertFrom-EnabledModData -EnabledModData $script:Entries[3] -IdIndex $script:IdIndex

        $mod.Id | Should -Be 'gamma'
        $mod.Name | Should -BeNullOrEmpty
        $mod.Version | Should -Be ''
        $mod.VersionInfo | Should -Be 'no parts'
    }

    It 'fails naming an entry that holds no spec' {

        { ConvertFrom-EnabledModData -EnabledModData $script:Entries[4] -IdIndex $script:IdIndex } |
            Should -Throw '<EnabledModData z="9"> holds no mod spec'
    }
}
