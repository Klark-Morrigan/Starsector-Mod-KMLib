BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\ConvertFrom-StarsectorCsv.ps1"

    function script:Read-CsvLines {
        # Reads the asked columns and writes each row as one line, values joined by |, a missing value as <null>.
        param(
            [string] $Text,
            [string[]] $ColumnNames
        )

        $rows = ConvertFrom-StarsectorCsv -Text $Text -ColumnNames $ColumnNames

        return @(foreach ($row in $rows) {
            ($row | ForEach-Object { if ($null -eq $_) { '<null>' } else { $_ } }) -join '|'
        })
    }
}

Describe 'ConvertFrom-StarsectorCsv' {

    It 'returns the asked columns of each row, in the order asked, rows in file order' {

        $text = "id,name,tier`nalpha,Alpha,1`nbeta,Beta,2`n"

        Read-CsvLines -Text $text -ColumnNames @('tier', 'id') | Should -Be @('1|alpha', '2|beta')
    }

    It 'reads a last row with no line break after it' {
        Read-CsvLines -Text "id`nalpha`nbeta" -ColumnNames @('id') | Should -Be @('alpha', 'beta')
    }

    It 'drops carriage returns' {
        Read-CsvLines -Text "name,id`r`nAlpha,alpha`r`n" -ColumnNames @('id') | Should -Be @('alpha')
    }

    It 'returns an empty array for <Description>' -ForEach @(
        @{ Description = 'a file of one line'; Text = 'id,name' }
        @{ Description = 'a header with only a line break after it'; Text = "id,name`n" }
        @{ Description = 'an empty file'; Text = '' }
    ) {

        $rows = ConvertFrom-StarsectorCsv -Text $Text -ColumnNames @('id')

        Should -ActualValue $rows -BeOfType [object[]]
        $rows.Count | Should -Be 0
    }

    It 'returns the rows as one array, however many there are' {

        $rows = ConvertFrom-StarsectorCsv -Text "id`nalpha`n" -ColumnNames @('id')

        $rows.Count | Should -Be 1
        $rows[0][0] | Should -Be 'alpha'
    }

    # LLI's wing_data.csv repeats a column name eleven times, which Import-Csv refuses.
    It 'reads a header that repeats a column name, taking the last column of that name' {
        Read-CsvLines -Text "id,02,02,code`na,first,second,c`n" -ColumnNames @('02', 'code') | Should -Be @('second|c')
    }

    It 'gives null for a column the header lacks, matching names case-sensitively' {
        Read-CsvLines -Text "ID,name`nalpha,Alpha`n" -ColumnNames @('id', 'name') | Should -Be @('<null>|Alpha')
    }

    It 'gives null for a column a short row ends before' {
        Read-CsvLines -Text "name,id`nalpha,a`nshort`n" -ColumnNames @('id') | Should -Be @('a', '<null>')
    }

    It 'trims header names and strips one enclosing quote, reading "" in a name as a quote' {

        $text = " `"id`" ,`"say `"`"hi`"`"`"`nalpha,x`n"

        Read-CsvLines -Text $text -ColumnNames @('id', 'say "hi"') | Should -Be @('alpha|x')
    }

    # The game splits the header on every comma, so a quoted comma there makes two names.
    It 'splits the header on a comma inside quotes' {
        Read-CsvLines -Text "`"a,b`",id`nx,y,alpha`n" -ColumnNames @('id') | Should -Be @('alpha')
    }

    It 'skips a row opening with # and an empty line, keeping a row that holds only commas' {
        Read-CsvLines -Text "id,name`n#gone,x`n`n,`nkept,y`n" -ColumnNames @('id') | Should -Be @('', 'kept')
    }

    It 'keeps a # that is not the first character of its row' {
        Read-CsvLines -Text "name,id`nx,#kept`n" -ColumnNames @('id') | Should -Be @('#kept')
    }

    It 'reads a comma and a line break inside quotes as text' {

        $text = "id,tags`n`"a,b`",`"one`ntwo`"`nc,d`n"

        Read-CsvLines -Text $text -ColumnNames @('id', 'tags') | Should -Be @("a,b|one`ntwo", 'c|d')
    }

    It 'reads <Raw> as <Expected>' -ForEach @(
        @{ Raw = '"a""b"'; Expected = 'a"b' }
        @{ Raw = 'a""b'; Expected = 'a"b' }
        @{ Raw = '""'; Expected = '"' }
        @{ Raw = 'a"b"c'; Expected = 'abc' }
        @{ Raw = ' a '; Expected = ' a ' }
    ) {
        Read-CsvLines -Text "id`n$Raw`n" -ColumnNames @('id') | Should -Be @($Expected)
    }

    It 'ignores values past the last header column' {
        Read-CsvLines -Text "id`nalpha,extra,more`n" -ColumnNames @('id') | Should -Be @('alpha')
    }

    It 'fails on a quote left open, naming where its row starts' {

        { ConvertFrom-StarsectorCsv -Text "id`nalpha`n`"open,b`n" -ColumnNames @('id') } |
            Should -Throw 'has a quote left open, at character 6 after the header'
    }
}
