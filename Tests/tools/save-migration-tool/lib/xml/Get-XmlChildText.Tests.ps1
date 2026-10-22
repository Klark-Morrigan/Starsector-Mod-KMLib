BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\Get-XmlChildText.ps1"
}

Describe 'Get-XmlChildText' {

    BeforeEach {

        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml('<spec><id>alpha</id><patch></patch><id>second</id></spec>')

        $script:Spec = $document.DocumentElement
    }

    It 'returns the text of the first child of that name' {
        Get-XmlChildText -Element $script:Spec -Name 'id' | Should -Be 'alpha'
    }

    It 'returns null for an empty child' {
        Get-XmlChildText -Element $script:Spec -Name 'patch' | Should -BeNullOrEmpty
    }

    It 'returns null for no child of that name' {
        Get-XmlChildText -Element $script:Spec -Name 'name' | Should -BeNullOrEmpty
    }
}
