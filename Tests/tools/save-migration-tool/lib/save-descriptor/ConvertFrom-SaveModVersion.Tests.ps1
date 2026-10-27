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

        # ARRANGE
        $expectedText = ''

        # ACT
        $actualText = ConvertFrom-SaveModVersion -VersionInfo $null

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'returns the string the mod gave, whatever the parts say' {

        # ARRANGE
        $versionInfo = New-VersionInfo '<versionInfo><major>1</major><string>1.14.1aa</string></versionInfo>'
        $expectedText = '1.14.1aa'

        # ACT
        $actualText = ConvertFrom-SaveModVersion -VersionInfo $versionInfo

        # ASSERT
        $actualText | Should -Be $expectedText
        Should -Invoke Format-ModVersion -Times 0
    }

    It 'formats the parts as Format-ModVersion does when the mod gave no string, an empty part as null' {

        # ARRANGE
        $versionInfo = New-VersionInfo '<versionInfo><major>1</major><minor>5</minor><patch></patch></versionInfo>'
        $expectedText = 'formatted-1-5-'

        # ACT
        $actualText = ConvertFrom-SaveModVersion -VersionInfo $versionInfo

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    # Format-ModVersion reads the parts only from the object form a parsed mod_info.json holds: a dictionary.
    It 'hands Format-ModVersion the parts as a dictionary' {

        # ARRANGE
        $versionInfo = New-VersionInfo '<versionInfo><major>1</major><minor>5</minor><patch>2</patch></versionInfo>'

        # ACT
        ConvertFrom-SaveModVersion -VersionInfo $versionInfo | Out-Null

        # ASSERT
        Should -Invoke Format-ModVersion -Times 1 -Exactly -ParameterFilter {
            $Version -is [System.Collections.IDictionary]
        }
    }

    It 'hands Format-ModVersion no parts at all for no string and no parts' {

        # ARRANGE
        $versionInfo = New-VersionInfo '<versionInfo><major></major><minor></minor><patch></patch></versionInfo>'
        $expectedText = 'formatted---'

        # ACT
        $actualText = ConvertFrom-SaveModVersion -VersionInfo $versionInfo

        # ASSERT
        $actualText | Should -Be $expectedText
        Should -Invoke Format-ModVersion -Times 1 -Exactly -ParameterFilter {
            $null -eq $Version.major -and $null -eq $Version.minor -and $null -eq $Version.patch
        }
    }
}
