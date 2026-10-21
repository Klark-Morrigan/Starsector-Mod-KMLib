BeforeAll {
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
        Resolve-XStreamReference -Element $script:Specs[0] -IdIndex $script:IdIndex | Should -Be $script:Specs[0]
    }

    It 'returns the element a ref points at' {
        Resolve-XStreamReference -Element $script:Specs[1] -IdIndex $script:IdIndex | Should -Be $script:Specs[0]
    }

    It 'fails naming a ref that points at no element' {

        { Resolve-XStreamReference -Element $script:Specs[2] -IdIndex $script:IdIndex } |
            Should -Throw '<spec ref="9"> points at no object: no element has z="9"'
    }
}
