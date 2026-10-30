BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\New-XStreamIdIndex.ps1"
}

Describe 'New-XStreamIdIndex' {

    It 'maps every z attribute, at any depth, to its element, and skips elements with none' {

        # ARRANGE
        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml('<root z="1"><list z="2"><item z="3"><st>x</st></item><item ref="3"/></list></root>')

        $expectedKeys = @('1', '2', '3')
        $expectedRootName = 'root'
        $expectedItemXml = '<item z="3"><st>x</st></item>'

        # ACT
        $actualIndex = New-XStreamIdIndex -Document $document

        # ASSERT
        $actualIndex.Keys | Sort-Object | Should -Be $expectedKeys
        $actualIndex['1'].Name | Should -Be $expectedRootName
        $actualIndex['3'].OuterXml | Should -Be $expectedItemXml
    }

    It 'returns an empty index for a document with no IDs' {

        # ARRANGE
        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml('<root><item/></root>')

        $expectedCount = 0

        # ACT
        $actualIndex = New-XStreamIdIndex -Document $document

        # ASSERT
        $actualIndex.Count | Should -Be $expectedCount
    }
}
