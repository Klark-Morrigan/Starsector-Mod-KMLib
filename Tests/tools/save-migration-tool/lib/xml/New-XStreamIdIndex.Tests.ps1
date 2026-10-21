BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\New-XStreamIdIndex.ps1"
}

Describe 'New-XStreamIdIndex' {

    It 'maps every z attribute, at any depth, to its element, and skips elements with none' {

        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml('<root z="1"><list z="2"><item z="3"><st>x</st></item><item ref="3"/></list></root>')

        $index = New-XStreamIdIndex -Document $document

        $index.Keys | Sort-Object | Should -Be @('1', '2', '3')
        $index['1'].Name | Should -Be 'root'
        $index['3'].OuterXml | Should -Be '<item z="3"><st>x</st></item>'
    }

    It 'returns an empty index for a document with no ids' {

        $document = [System.Xml.XmlDocument]::new()
        $document.LoadXml('<root><item/></root>')

        (New-XStreamIdIndex -Document $document).Count | Should -Be 0
    }
}
