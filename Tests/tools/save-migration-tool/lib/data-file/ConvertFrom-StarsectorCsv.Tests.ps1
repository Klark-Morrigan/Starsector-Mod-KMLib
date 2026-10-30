BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\data-file\ConvertFrom-StarsectorCsv.ps1"
    . "$libRoot\data-file\ConvertTo-StraightQuotes.ps1"
    . "$libRoot\data-file\Read-StarsectorText.ps1"

    function script:Read-CsvLines {
        # Reads the text and writes each row as one line, its values joined by |.
        param([string] $Text)

        $table = ConvertFrom-StarsectorCsv -Text $Text

        return @(foreach ($row in $table.Rows) {
            $row -join '|'
        })
    }

    function script:Format-ColumnIndexes {
        # Writes the header's column indexes as one line, names sorted: "id=0 name=1".
        param([object] $Table)

        $names = [string[]] $Table.ColumnIndexByName.Keys
        [System.Array]::Sort($names, [System.StringComparer]::Ordinal)

        return ($names | ForEach-Object { "$_=$($Table.ColumnIndexByName[$_])" }) -join ' '
    }
}

Describe 'ConvertFrom-StarsectorCsv' {

    It 'returns every value of each row, rows in file order, and each column''s index by name' {

        # ARRANGE
        $text = "id,name,tier`nalpha,Alpha,1`nbeta,Beta,2`n"

        $expectedLines = @('alpha|Alpha|1', 'beta|Beta|2')
        $expectedIndexes = 'id=0 name=1 tier=2'

        # ACT
        $actualTable = ConvertFrom-StarsectorCsv -Text $text

        # ASSERT
        @($actualTable.Rows | ForEach-Object { $_ -join '|' }) | Should -Be $expectedLines
        Format-ColumnIndexes -Table $actualTable | Should -Be $expectedIndexes
    }

    It 'reads the file at a path when given one' {

        # ARRANGE
        $path = 'data\sheet.csv'

        Mock Read-StarsectorText { "id`nfrom_file`n" }

        $expectedLines = @('from_file')

        # ACT
        $actualTable = ConvertFrom-StarsectorCsv -Path $path

        # ASSERT
        @($actualTable.Rows | ForEach-Object { $_ -join '|' }) | Should -Be $expectedLines
        Should -Invoke Read-StarsectorText -ParameterFilter { $Path -eq $path } -Times 1 -Exactly
    }

    It 'straightens curly quotes before it reads the text, when asked' {

        # ARRANGE
        Mock ConvertTo-StraightQuotes { "id`nstraightened`n" }

        $expectedLines = @('straightened')

        # ACT
        $actualTable = ConvertFrom-StarsectorCsv -Text "id`ncurly`n" -ConvertsCurlyQuotes

        # ASSERT
        @($actualTable.Rows | ForEach-Object { $_ -join '|' }) | Should -Be $expectedLines
        Should -Invoke ConvertTo-StraightQuotes -ParameterFilter { $Text -eq "id`ncurly`n" } -Times 1 -Exactly
    }

    It 'leaves curly quotes alone when not asked' {

        # ARRANGE
        Mock ConvertTo-StraightQuotes { $Text }

        # ACT
        ConvertFrom-StarsectorCsv -Text "id`nalpha`n" | Out-Null

        # ASSERT
        Should -Invoke ConvertTo-StraightQuotes -Times 0 -Exactly
    }

    It 'reads a last row with no line break after it' {

        # ARRANGE
        $expectedLines = @('alpha', 'beta')

        # ACT
        $actualLines = Read-CsvLines -Text "id`nalpha`nbeta"

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'drops carriage returns' {

        # ARRANGE
        $expectedLines = @('Alpha|alpha')

        # ACT
        $actualLines = Read-CsvLines -Text "name,id`r`nAlpha,alpha`r`n"

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'returns no rows for <Description>' -ForEach @(
        @{ Description = 'a file of one line'; Text = 'id,name' }
        @{ Description = 'a header with only a line break after it'; Text = "id,name`n" }
        @{ Description = 'an empty file'; Text = '' }
    ) {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualTable = ConvertFrom-StarsectorCsv -Text $Text

        # ASSERT
        Should -ActualValue $actualTable.Rows -BeOfType [object[]]
        $actualTable.Rows.Count | Should -Be $expectedCount
    }

    It 'returns a row of one value as an array, in an array of rows' {

        # ARRANGE
        $expectedRowCount = 1
        $expectedValueCount = 1
        $expectedValue = 'alpha'

        # ACT
        $actualTable = ConvertFrom-StarsectorCsv -Text "id`nalpha`n"

        # ASSERT
        $actualTable.Rows.Count | Should -Be $expectedRowCount
        $actualTable.Rows[0].Count | Should -Be $expectedValueCount
        $actualTable.Rows[0][0] | Should -Be $expectedValue
    }

    # LLI's wing_data.csv repeats a column name eleven times, which Import-Csv refuses.
    It 'reads a header that repeats a column name, indexing the last column of that name' {

        # ARRANGE
        $expectedIndexes = '02=2 code=3 id=0'

        # ACT
        $actualTable = ConvertFrom-StarsectorCsv -Text "id,02,02,code`na,first,second,c`n"

        # ASSERT
        Format-ColumnIndexes -Table $actualTable | Should -Be $expectedIndexes
    }

    It 'indexes column names case-sensitively' {

        # ARRANGE
        $expectedIndexes = 'ID=0 id=1'

        # ACT
        $actualTable = ConvertFrom-StarsectorCsv -Text "ID,id`nalpha,beta`n"

        # ASSERT
        Format-ColumnIndexes -Table $actualTable | Should -Be $expectedIndexes
    }

    It 'keeps only the values a short row holds' {

        # ARRANGE
        $expectedLines = @('alpha|a', 'short')

        # ACT
        $actualLines = Read-CsvLines -Text "name,id`nalpha,a`nshort`n"

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'trims header names and strips one enclosing quote, reading "" in a name as a quote' {

        # ARRANGE
        $text = " `"id`" ,`"say `"`"hi`"`"`"`nalpha,x`n"

        $expectedIndexes = 'id=0 say "hi"=1'

        # ACT
        $actualTable = ConvertFrom-StarsectorCsv -Text $text

        # ASSERT
        Format-ColumnIndexes -Table $actualTable | Should -Be $expectedIndexes
    }

    # The game splits the header on every comma, so a quoted comma there makes two names.
    It 'splits the header on a comma inside quotes' {

        # ARRANGE
        $expectedIndexes = 'a=0 b=1 id=2'

        # ACT
        $actualTable = ConvertFrom-StarsectorCsv -Text "`"a,b`",id`nx,y,alpha`n"

        # ASSERT
        Format-ColumnIndexes -Table $actualTable | Should -Be $expectedIndexes
    }

    It 'skips a row opening with # and an empty line, keeping a row that holds only commas' {

        # ARRANGE
        $expectedLines = @('|', 'kept|y')

        # ACT
        $actualLines = Read-CsvLines -Text "id,name`n#gone,x`n`n,`nkept,y`n"

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'keeps a # that is not the first character of its row' {

        # ARRANGE
        $expectedLines = @('x|#kept')

        # ACT
        $actualLines = Read-CsvLines -Text "name,id`nx,#kept`n"

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'reads a comma and a line break inside quotes as text' {

        # ARRANGE
        $text = "id,tags`n`"a,b`",`"one`ntwo`"`nc,d`n"

        $expectedLines = @("a,b|one`ntwo", 'c|d')

        # ACT
        $actualLines = Read-CsvLines -Text $text

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
        $actualLines = Read-CsvLines -Text "id`n$Raw`n"

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'keeps values past the last header column' {

        # ARRANGE
        $expectedLines = @('alpha|extra|more')

        # ACT
        $actualLines = Read-CsvLines -Text "id`nalpha,extra,more`n"

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'fails on a quote left open, naming where its row starts' {

        # ARRANGE
        $expectedMessage = 'has a quote left open, at character 6 after the header'

        # ACT
        $actualError = { ConvertFrom-StarsectorCsv -Text "id`nalpha`n`"open,b`n" } | Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -Be $expectedMessage
    }
}
