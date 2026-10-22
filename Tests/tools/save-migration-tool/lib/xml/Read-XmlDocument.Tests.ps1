BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\Read-XmlDocument.ps1"
}

Describe 'Read-XmlDocument' {

    It 'loads the file whole' {

        $path = Join-Path $TestDrive 'plain.xml'

        [System.IO.File]::WriteAllText($path, '<?xml version="1.0" ?><a z="1"><b>text</b></a>')

        $document = Read-XmlDocument -Path $path

        $document | Should -BeOfType [System.Xml.XmlDocument]
        $document.DocumentElement.SelectSingleNode('b').InnerText | Should -Be 'text'
    }

    It 'refuses a file that declares a DTD' {

        $path = Join-Path $TestDrive 'dtd.xml'

        [System.IO.File]::WriteAllText($path, '<?xml version="1.0" ?><!DOCTYPE a [<!ENTITY e "x">]><a>&e;</a>')

        { Read-XmlDocument -Path $path } | Should -Throw '*DTD is prohibited*'
    }
}
