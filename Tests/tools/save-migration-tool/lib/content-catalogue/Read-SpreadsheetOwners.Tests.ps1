BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\content-catalogue\Add-ContentOwner.ps1"
    . "$libRoot\content-catalogue\ConvertFrom-StarsectorCsv.ps1"
    . "$libRoot\content-catalogue\ConvertTo-StraightQuotes.ps1"
    . "$libRoot\content-catalogue\New-SpreadsheetSource.ps1"
    . "$libRoot\content-catalogue\Read-SpreadsheetOwners.ps1"
    . "$libRoot\mod-info\Read-StarsectorText.ps1"

    function script:New-Source {
        # One data folder under the case's own folder; its spreadsheet is written when -HasSpreadsheet is given.
        param(
            [string] $Owner,
            [string[]] $ReplacedPaths = @(),
            [switch] $HasSpreadsheet
        )

        $folder = Join-Path $TestDrive "$script:Case\$Owner"

        if ($HasSpreadsheet) {
            New-TestFile -Name "$script:Case\$Owner\data\sheet.csv" | Out-Null
        }

        return [pscustomobject]@{ Owner = $Owner; Folder = $folder; ReplacedPaths = $ReplacedPaths }
    }
}

Describe 'Read-SpreadsheetOwners' {

    BeforeEach {

        $script:Case = [guid]::NewGuid().ToString('N')
        $script:Added = [System.Collections.Generic.List[string]]::new()

        # Each copy's rows, by owner, as the reader returns them: one array of the asked columns per row.
        $script:RowsByOwner = @{}

        # Each copy's text is its owner's name, so the reader fake hands back that owner's rows.
        Mock Read-StarsectorText { Split-Path -Leaf ($Path -replace '[\\/]data[\\/]sheet\.csv$', '') }
        Mock ConvertFrom-StarsectorCsv { ,[object[]] $script:RowsByOwner[$Text] }
        Mock ConvertTo-StraightQuotes { $Text }
        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }
    }

    # Removing one owner of an ID leaves it defined while another's row defines it too.
    It 'reads every copy in search order, owning an ID by every copy defining it, skipping a source without one' {

        $script:RowsByOwner['alpha'] = @(, @('shared'))
        $script:RowsByOwner['core'] = @(@('shared'), @('core_only'))

        $sources = @(
            (New-Source -Owner 'alpha' -HasSpreadsheet)
            (New-Source -Owner 'beta')
            (New-Source -Owner 'core' -HasSpreadsheet)
        )

        $read = Read-SpreadsheetOwners -Sources $sources -Spreadsheet (New-SpreadsheetSource -Path 'data/sheet.csv')

        $script:Added | Should -Be @('shared=alpha', 'shared=core', 'core_only=core')
        $read.FileCount | Should -Be 2
        $read.ReplacingOwner | Should -BeNullOrEmpty

        Should -Invoke ConvertFrom-StarsectorCsv -ParameterFilter { "$ColumnNames" -eq 'id' } -Times 2 -Exactly
        Should -Invoke ConvertTo-StraightQuotes -Times 0 -Exactly
    }

    It 'reads only the copy of the first source that replaces the spreadsheet, and names it' {

        foreach ($owner in 'alpha', 'beta', 'gamma', 'core') {
            $script:RowsByOwner[$owner] = @(, @("$owner-row"))
        }

        $sources = @(
            (New-Source -Owner 'alpha' -HasSpreadsheet)
            (New-Source -Owner 'beta' -HasSpreadsheet -ReplacedPaths @('data/sheet.csv'))
            (New-Source -Owner 'gamma' -HasSpreadsheet -ReplacedPaths @('data/sheet.csv'))
            (New-Source -Owner 'core' -HasSpreadsheet)
        )

        $read = Read-SpreadsheetOwners -Sources $sources -Spreadsheet (New-SpreadsheetSource -Path 'data/sheet.csv')

        $script:Added | Should -Be @('beta-row=beta')
        $read.ReplacingOwner | Should -Be 'beta'
    }

    # The game compares a replaced spreadsheet's path as written, unlike a replaced JSON file's.
    It 'reads every copy when the replaced path is written with backslashes' {

        $script:RowsByOwner['alpha'] = @(, @('a'))
        $script:RowsByOwner['core'] = @(, @('c'))

        $sources = @(
            (New-Source -Owner 'alpha' -HasSpreadsheet -ReplacedPaths @('data\sheet.csv'))
            (New-Source -Owner 'core' -HasSpreadsheet)
        )

        $read = Read-SpreadsheetOwners -Sources $sources -Spreadsheet (New-SpreadsheetSource -Path 'data/sheet.csv')

        $script:Added | Should -Be @('a=alpha', 'c=core')
        $read.ReplacingOwner | Should -BeNullOrEmpty
    }

    # A row whose key is empty is no row to the game; one whose ID is empty claims its key and defines nothing.
    It 'drops a row with an empty key, and records no empty ID' {

        $script:RowsByOwner['alpha'] = @(@('', ''), @('row', ''), @('kept', 'id_kept'))
        $script:RowsByOwner['core'] = @(@('', 'from_empty_key'), @('row', 'later'))

        $sources = @((New-Source -Owner 'alpha' -HasSpreadsheet), (New-Source -Owner 'core' -HasSpreadsheet))
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -IdColumn 'effect'

        Read-SpreadsheetOwners -Sources $sources -Spreadsheet $spreadsheet | Out-Null

        $script:Added | Should -Be @('id_kept=alpha')

        Should -Invoke ConvertFrom-StarsectorCsv -ParameterFilter { "$ColumnNames" -eq 'id effect' }
    }

    # The game merges by the key first, then filters, so a filtered row still claims its key,
    # and a later copy's row for that key defines nothing, though it passes the filter.
    It 'keeps a row only when its filter column holds the value, after the merge' {

        $script:RowsByOwner['alpha'] = @(@('star', 'STAR'), @('rock', 'PLANET'), @('none', $null))
        $script:RowsByOwner['core'] = @(@('star', 'PLANET'), @('ice', 'PLANET'))

        $sources = @((New-Source -Owner 'alpha' -HasSpreadsheet), (New-Source -Owner 'core' -HasSpreadsheet))
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -RowFilterColumn 'type' -RowFilterValue 'PLANET'

        Read-SpreadsheetOwners -Sources $sources -Spreadsheet $spreadsheet | Out-Null

        $script:Added | Should -Be @('rock=alpha', 'ice=core')

        Should -Invoke ConvertFrom-StarsectorCsv -ParameterFilter { "$ColumnNames" -eq 'id type' }
    }

    It 'joins several key columns into the ID, skipping one opening with # and one missing a part when asked' {

        $script:RowsByOwner['alpha'] = @(@('a', 'x'), @('#b', 'x'), @('c', ''))

        $joined = New-SpreadsheetSource -Path 'data/sheet.csv' -KeyColumns @('name', 'tags') -SkipsHashIds
        $complete = New-SpreadsheetSource -Path 'data/sheet.csv' -KeyColumns @('name', 'tags') -IdJoiner '_' `
            -RequiresEveryKeyColumn

        Read-SpreadsheetOwners -Sources @(New-Source -Owner 'alpha' -HasSpreadsheet) -Spreadsheet $joined | Out-Null
        Read-SpreadsheetOwners -Sources @(New-Source -Owner 'alpha' -HasSpreadsheet) -Spreadsheet $complete | Out-Null

        $script:Added | Should -Be @('a_|_x=alpha', 'c_|_=alpha', 'a_x=alpha', '#b_x=alpha')
    }

    It 'straightens curly quotes before it reads the text, when the source says so' {

        $script:RowsByOwner['alpha'] = @(, @('a'))

        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -ConvertsCurlyQuotes

        Read-SpreadsheetOwners -Sources @(New-Source -Owner 'alpha' -HasSpreadsheet) -Spreadsheet $spreadsheet |
            Out-Null

        Should -Invoke ConvertTo-StraightQuotes -ParameterFilter { $Text -eq 'alpha' } -Times 1 -Exactly
    }

    It 'skips a copy lacking a column it reads in some row, or that does not parse, and reads the others' {

        Mock ConvertFrom-StarsectorCsv {

            if ($Text -eq 'beta') {
                throw 'has a quote left open, at character 3 after the header'
            }

            ,[object[]] $script:RowsByOwner[$Text]
        }

        $script:RowsByOwner['alpha'] = @(@('a', 'x'), @('b', $null))
        $script:RowsByOwner['core'] = @(, @('c', 'y'))

        $sources = @(
            (New-Source -Owner 'alpha' -HasSpreadsheet)
            (New-Source -Owner 'beta' -HasSpreadsheet)
            (New-Source -Owner 'core' -HasSpreadsheet)
        )

        $read = Read-SpreadsheetOwners -Sources $sources -Spreadsheet (
            New-SpreadsheetSource -Path 'data/sheet.csv' -IdColumn 'effect')

        $script:Added | Should -Be @('y=core')
        $read.FileCount | Should -Be 3
        $read.SkippedFiles.Path | Should -Be @(
            "$($sources[0].Folder)\data\sheet.csv"
            "$($sources[1].Folder)\data\sheet.csv"
        )
        $read.SkippedFiles.Reason | Should -Be @(
            'has no effect column in every row'
            'has a quote left open, at character 3 after the header'
        )
    }
}
