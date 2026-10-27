BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\New-VersionInfo.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\Get-XmlChildText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-descriptor\ConvertFrom-SaveVersionInfo.ps1"

    function script:New-VersionInfoElement {
        # Parses one versionInfo element written as XStream writes it.
        param([string] $Xml)

        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml($Xml)

        return $document.DocumentElement
    }
}

Describe 'ConvertFrom-SaveVersionInfo' {

    BeforeEach {

        # Reads the child as the real function does, so each case states its versionInfo as XML.
        Mock Get-XmlChildText {

            $child = $Element.SelectSingleNode($Name)
            if ($null -ne $child -and $child.InnerText) { $child.InnerText }
        }

        Mock New-VersionInfo { "$Major|$Minor|$Patch|$MinorIsRC" }
    }

    It 'takes the parts as the save wrote them, ignoring the version text' {

        $versionInfo = New-VersionInfoElement (
            '<versionInfo><major>0.1</major><minor>98</minor><patch>7</patch>' +
            '<string>something else</string><minorIsRC>false</minorIsRC></versionInfo>')

        ConvertFrom-SaveVersionInfo -VersionInfo $versionInfo | Should -Be '0.1|98|7|False'
    }

    It 'reads a minor part the save marks as a release candidate' {

        $versionInfo = New-VersionInfoElement (
            '<versionInfo><major>0.98</major><minorIsRC>true</minorIsRC></versionInfo>')

        ConvertFrom-SaveVersionInfo -VersionInfo $versionInfo | Should -Be '0.98|||True'
    }

    It 'reads empty and absent parts as none' {

        $versionInfo = New-VersionInfoElement '<versionInfo><major></major><string>2025-03-29</string></versionInfo>'

        ConvertFrom-SaveVersionInfo -VersionInfo $versionInfo | Should -Be '|||False'
    }

    It 'reads no versionInfo as no parts' {
        ConvertFrom-SaveVersionInfo -VersionInfo $null | Should -Be '|||False'
    }
}
