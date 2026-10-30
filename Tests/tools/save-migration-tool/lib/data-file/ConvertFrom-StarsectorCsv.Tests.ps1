BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\ConvertFrom-StarsectorCsv.ps1"

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

        # ARRANGE
        $text = "id,name,tier`nalpha,Alpha,1`nbeta,Beta,2`n"
        $expectedLines = @('1|alpha', '2|beta')

        # ACT
        $actualLines = Read-CsvLines -Text $text -ColumnNames @('tier', 'id')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'reads a last row with no line break after it' {

        # ARRANGE
        $expectedLines = @('alpha', 'beta')

        # ACT
        $actualLines = Read-CsvLines -Text "id`nalpha`nbeta" -ColumnNames @('id')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'drops carriage returns' {

        # ARRANGE
        $expectedLines = @('alpha')

        # ACT
        $actualLines = Read-CsvLines -Text "name,id`r`nAlpha,alpha`r`n" -ColumnNames @('id')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'returns an empty array for <Description>' -ForEach @(
        @{ Description = 'a file of one line'; Text = 'id,name' }
        @{ Description = 'a header with only a line break after it'; Text = "id,name`n" }
        @{ Description = 'an empty file'; Text = '' }
    ) {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualRows = ConvertFrom-StarsectorCsv -Text $Text -ColumnNames @('id')

        # ASSERT
        Should -ActualValue $actualRows -BeOfType [object[]]
        $actualRows.Count | Should -Be $expectedCount
    }

    It 'returns the rows as one array, however many there are' {

        # ARRANGE
        $expectedCount = 1
        $expectedFirstValue = 'alpha'

        # ACT
        $actualRows = ConvertFrom-StarsectorCsv -Text "id`nalpha`n" -ColumnNames @('id')

        # ASSERT
        $actualRows.Count | Should -Be $expectedCount
        $actualRows[0][0] | Should -Be $expectedFirstValue
    }

    # LLI's wing_data.csv repeats a column name eleven times, which Import-Csv refuses.
    It 'reads a header that repeats a column name, taking the last column of that name' {

        # ARRANGE
        $expectedLines = @('second|c')

        # ACT
        $actualLines = Read-CsvLines -Text "id,02,02,code`na,first,second,c`n" -ColumnNames @('02', 'code')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'gives null for a column the header lacks, matching names case-sensitively' {

        # ARRANGE
        $expectedLines = @('<null>|Alpha')

        # ACT
        $actualLines = Read-CsvLines -Text "ID,name`nalpha,Alpha`n" -ColumnNames @('id', 'name')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'gives null for a column a short row ends before' {

        # ARRANGE
        $expectedLines = @('a', '<null>')

        # ACT
        $actualLines = Read-CsvLines -Text "name,id`nalpha,a`nshort`n" -ColumnNames @('id')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'trims header names and strips one enclosing quote, reading "" in a name as a quote' {

        # ARRANGE
        $text = " `"id`" ,`"say `"`"hi`"`"`"`nalpha,x`n"
        $expectedLines = @('alpha|x')

        # ACT
        $actualLines = Read-CsvLines -Text $text -ColumnNames @('id', 'say "hi"')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    # The game splits the header on every comma, so a quoted comma there makes two names.
    It 'splits the header on a comma inside quotes' {

        # ARRANGE
        $expectedLines = @('alpha')

        # ACT
        $actualLines = Read-CsvLines -Text "`"a,b`",id`nx,y,alpha`n" -ColumnNames @('id')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'skips a row opening with # and an empty line, keeping a row that holds only commas' {

        # ARRANGE
        $expectedLines = @('', 'kept')

        # ACT
        $actualLines = Read-CsvLines -Text "id,name`n#gone,x`n`n,`nkept,y`n" -ColumnNames @('id')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'keeps a # that is not the first character of its row' {

        # ARRANGE
        $expectedLines = @('#kept')

        # ACT
        $actualLines = Read-CsvLines -Text "name,id`nx,#kept`n" -ColumnNames @('id')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'reads a comma and a line break inside quotes as text' {

        # ARRANGE
        $text = "id,tags`n`"a,b`",`"one`ntwo`"`nc,d`n"
        $expectedLines = @("a,b|one`ntwo", 'c|d')

        # ACT
        $actualLines = Read-CsvLines -Text $text -ColumnNames @('id', 'tags')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'reads <Raw> as <Expected>' -ForEach @(
        @{ Raw = '"a""b"'; Expected = 'a"b' }
        @{ Raw = 'a""b'; Expected = 'a"b' }
        @{ Raw = '""'; Expected = '"' }
        @{ Raw = 'a"b"c'; Expected = 'abc' }
        @{ Raw = ' a '; Expected = ' a ' }
    ) {

        # ARRANGE
        $expectedLines = @($Expected)

        # ACT
        $actualLines = Read-CsvLines -Text "id`n$Raw`n" -ColumnNames @('id')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'ignores values past the last header column' {

        # ARRANGE
        $expectedLines = @('alpha')

        # ACT
        $actualLines = Read-CsvLines -Text "id`nalpha,extra,more`n" -ColumnNames @('id')

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'fails on a quote left open, naming where its row starts' {

        # ARRANGE
        $expectedMessage = 'has a quote left open, at character 6 after the header'

        # ACT
        $actualError = { ConvertFrom-StarsectorCsv -Text "id`nalpha`n`"open,b`n" -ColumnNames @('id') } |
            Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -Be $expectedMessage
    }
}
