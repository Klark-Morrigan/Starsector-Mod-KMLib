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

        # ARRANGE
        $expectedText = 'alpha'

        # ACT
        $actualText = Get-XmlChildText -Element $script:Spec -Name 'id'

        # ASSERT
        $actualText | Should -Be $expectedText
    }

    It 'returns null for an empty child' {

        # ACT
        $actualText = Get-XmlChildText -Element $script:Spec -Name 'patch'

        # ASSERT
        $actualText | Should -BeNullOrEmpty
    }

    It 'returns null for no child of that name' {

        # ACT
        $actualText = Get-XmlChildText -Element $script:Spec -Name 'name'

        # ASSERT
        $actualText | Should -BeNullOrEmpty
    }
}
