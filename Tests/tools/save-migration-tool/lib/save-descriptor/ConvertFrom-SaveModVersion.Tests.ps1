BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Format-ModVersion.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\Get-XmlChildText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\save-descriptor\ConvertFrom-SaveModVersion.ps1"

    function script:New-VersionInfo {
        # Parses one versionInfo element written as XStream writes it.
        param([string] $Xml)

        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml($Xml)

        return $document.DocumentElement
    }
}

Describe 'ConvertFrom-SaveModVersion' {

    BeforeEach {

        # Reads the child as the real function does, so each case states its versionInfo as XML.
        Mock Get-XmlChildText {

            $child = $Element.SelectSingleNode($Name)
            if ($null -ne $child -and $child.InnerText) { $child.InnerText }
        }

        Mock Format-ModVersion { "formatted-$($Version.major)-$($Version.minor)-$($Version.patch)" }
    }

    It 'returns an empty string for no versionInfo' {
        ConvertFrom-SaveModVersion -VersionInfo $null | Should -Be ''
    }

    It 'returns the string the mod gave, whatever the parts say' {

        $versionInfo = New-VersionInfo '<versionInfo><major>1</major><string>1.14.1aa</string></versionInfo>'

        ConvertFrom-SaveModVersion -VersionInfo $versionInfo | Should -Be '1.14.1aa'
        Should -Invoke Format-ModVersion -Times 0
    }

    It 'formats the parts as Format-ModVersion does when the mod gave no string, an empty part as null' {

        $versionInfo = New-VersionInfo '<versionInfo><major>1</major><minor>5</minor><patch></patch></versionInfo>'

        ConvertFrom-SaveModVersion -VersionInfo $versionInfo | Should -Be 'formatted-1-5-'
    }

    It 'hands Format-ModVersion no parts at all for no string and no parts' {

        $versionInfo = New-VersionInfo '<versionInfo><major></major><minor></minor><patch></patch></versionInfo>'

        ConvertFrom-SaveModVersion -VersionInfo $versionInfo | Should -Be 'formatted---'

        Should -Invoke Format-ModVersion -Times 1 -Exactly -ParameterFilter {
            $null -eq $Version.major -and $null -eq $Version.minor -and $null -eq $Version.patch
        }
    }
}
