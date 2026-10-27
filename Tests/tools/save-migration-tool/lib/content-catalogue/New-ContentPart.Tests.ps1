BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\New-ContentPart.ps1"
}

Describe 'New-ContentPart' {

    It 'holds no source of a kind it is not given' {

        $part = New-ContentPart

        $part.FileSets.Count | Should -Be 0
        $part.Spreadsheets.Count | Should -Be 0
        $part.KeyedFiles.Count | Should -Be 0
        $part.BuiltInIds.Count | Should -Be 0
    }

    It 'holds each source it is given' {

        $part = New-ContentPart -FileSets @('files') -Spreadsheets @('rows') -KeyedFiles @('keyed') `
            -BuiltInIds @('omen1')

        $part.FileSets | Should -Be @('files')
        $part.Spreadsheets | Should -Be @('rows')
        $part.KeyedFiles | Should -Be @('keyed')
        $part.BuiltInIds | Should -Be @('omen1')
    }
}
