BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\xml\Read-XmlDocument.ps1"
}

Describe 'Read-XmlDocument' {

    It 'loads the file whole' {

        # ARRANGE
        $path = Join-Path $TestDrive 'plain.xml'

        [System.IO.File]::WriteAllText($path, '<?xml version="1.0" ?><a z="1"><b>text</b></a>')

        $expectedText = 'text'

        # ACT
        $actualDocument = Read-XmlDocument -Path $path

        # ASSERT
        $actualDocument | Should -BeOfType [System.Xml.XmlDocument]
        $actualDocument.DocumentElement.SelectSingleNode('b').InnerText | Should -Be $expectedText
    }

    It 'refuses a file that declares a DTD' {

        # ARRANGE
        $path = Join-Path $TestDrive 'dtd.xml'

        [System.IO.File]::WriteAllText($path, '<?xml version="1.0" ?><!DOCTYPE a [<!ENTITY e "x">]><a>&e;</a>')

        $expectedMessagePattern = '*DTD is prohibited*'

        # ACT
        $actualError = { Read-XmlDocument -Path $path } | Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -BeLike $expectedMessagePattern
    }
}
