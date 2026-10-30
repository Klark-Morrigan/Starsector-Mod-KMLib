BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\Resolve-XStreamReference.ps1"
}

Describe 'Resolve-XStreamReference' {

    BeforeEach {

        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml('<root><spec z="5"><id>alpha</id></spec><spec ref="5"/><spec ref="9"/></root>')

        $script:Specs = $document.DocumentElement.SelectNodes('spec')
        $script:IdIndex = @{ '5' = $script:Specs[0] }
    }

    It 'returns an element with no ref as it is' {

        # ARRANGE
        $expectedElement = $script:Specs[0]

        # ACT
        $actualElement = Resolve-XStreamReference -Element $script:Specs[0] -IdIndex $script:IdIndex

        # ASSERT
        $actualElement | Should -Be $expectedElement
    }

    It 'returns the element a ref points at' {

        # ARRANGE
        $expectedElement = $script:Specs[0]

        # ACT
        $actualElement = Resolve-XStreamReference -Element $script:Specs[1] -IdIndex $script:IdIndex

        # ASSERT
        $actualElement | Should -Be $expectedElement
    }

    It 'fails naming a ref that points at no element' {

        # ARRANGE
        $expectedMessage = '<spec ref="9"> points at no object: no element has z="9"'

        # ACT
        $actualError = { Resolve-XStreamReference -Element $script:Specs[2] -IdIndex $script:IdIndex } |
            Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -Be $expectedMessage
    }
}
