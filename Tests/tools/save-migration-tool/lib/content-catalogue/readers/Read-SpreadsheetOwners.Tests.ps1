BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\content-catalogue\owner-index\Add-ContentOwner.ps1"
    . "$libRoot\content-catalogue\readers\ConvertTo-SpreadsheetRowIds.ps1"
    . "$libRoot\content-catalogue\readers\Find-ReplacingSource.ps1"
    . "$libRoot\content-catalogue\readers\Invoke-CachedRead.ps1"
    . "$libRoot\content-catalogue\owner-index\New-ContentOwnerIndex.ps1"
    . "$libRoot\content-catalogue\definitions\New-SpreadsheetSource.ps1"
    . "$libRoot\content-catalogue\readers\Read-SpreadsheetOwners.ps1"
    . "$libRoot\data-file\Select-CsvColumns.ps1"

    function script:New-Source {
        # One data folder under the case's own folder; its spreadsheet is written when -HasSpreadsheet is given.
        param(
            [string] $Owner,
            [switch] $HasSpreadsheet
        )

        $folder = Join-Path $TestDrive "$script:Case\$Owner"

        if ($HasSpreadsheet) {
            New-TestFile -Name "$script:Case\$Owner\data\sheet.csv" | Out-Null
        }

        return [pscustomobject]@{ Owner = $Owner; Folder = $folder }
    }

    function script:New-CopyRows {
        # One copy's rows as ConvertTo-SpreadsheetRowIds hands them back: each key, and its ID or empty, by index.
        param(
            [string[]] $Keys,
            [string[]] $Ids
        )

        return [pscustomobject]@{ Keys = $Keys; Ids = $Ids }
    }

    function script:Read-Spreadsheet {
        # Reads the spreadsheet across the sources with the case's cache.
        param(
            [object[]] $Sources,
            [object] $Spreadsheet
        )

        return Read-SpreadsheetOwners -Sources $Sources -Spreadsheet $Spreadsheet -Cache $script:Cache
    }
}

Describe 'Read-SpreadsheetOwners' {

    BeforeEach {

        $script:Case = [guid]::NewGuid().ToString('N')
        $script:Added = [System.Collections.Generic.List[string]]::new()
        $script:Cache = [pscustomobject]@{
            SpreadsheetsByKey = [System.Collections.Generic.Dictionary[string, object]]::new()
        }

        # Each copy's rows, by owner, as the row converter fake hands them back.
        $script:CopyRowsByOwner = @{}

        # Each copy parses to its owner's name, and its one row is that name,
        # so the row converter fake hands back that owner's rows.
        Mock Invoke-CachedRead { Split-Path -Leaf ($Parameters.Path -replace '[\\/]data[\\/]sheet\.csv$', '') }
        Mock Select-CsvColumns { ,[object[]] @($Table) }
        Mock ConvertTo-SpreadsheetRowIds { $script:CopyRowsByOwner["$Rows"] }
        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }
        Mock Find-ReplacingSource { $null }
    }

    # Removing one owner of an ID leaves it defined while another's row defines it too.
    It 'reads every copy in search order, owning an ID by every copy defining it, skipping a source without one' {

        # ARRANGE
        $script:CopyRowsByOwner['alpha'] = New-CopyRows -Keys @('shared') -Ids @('shared')
        $script:CopyRowsByOwner['core'] = New-CopyRows -Keys @('shared', 'core_only') -Ids @('shared', 'core_only')

        $sources = @(
            (New-Source -Owner 'alpha' -HasSpreadsheet)
            (New-Source -Owner 'beta')
            (New-Source -Owner 'core' -HasSpreadsheet)
        )
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -IdColumn 'effect'

        $expectedAdded = @('shared=alpha', 'shared=core', 'core_only=core')
        $expectedColumnNames = 'id effect'
        $expectedSpreadsheet = $spreadsheet

        # ACT
        $actualRead = Read-Spreadsheet -Sources $sources -Spreadsheet $spreadsheet

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.Replacement | Should -BeNullOrEmpty

        Should -Invoke Select-CsvColumns -Times 2 -Exactly -ParameterFilter {
            "$ColumnNames" -ceq $expectedColumnNames
        }
        Should -Invoke ConvertTo-SpreadsheetRowIds -Times 2 -Exactly -ParameterFilter {
            [object]::ReferenceEquals($Spreadsheet, $expectedSpreadsheet)
        }
    }

    # A spreadsheet several kinds read is parsed once for all of them.
    It 'parses each copy through the run''s cache, keyed by its path and its quote handling' {

        # ARRANGE
        $script:CopyRowsByOwner['alpha'] = New-CopyRows -Keys @('a') -Ids @('a')

        $sources = @(New-Source -Owner 'alpha' -HasSpreadsheet)
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -ConvertsCurlyQuotes

        $expectedPath = "$($sources[0].Folder)\data\sheet.csv"
        $expectedKey = "$expectedPath`nTrue"
        $expectedResults = $script:Cache.SpreadsheetsByKey

        # ACT
        Read-Spreadsheet -Sources $sources -Spreadsheet $spreadsheet | Out-Null

        # ASSERT
        Should -Invoke Invoke-CachedRead -Times 1 -Exactly -ParameterFilter {
            [object]::ReferenceEquals($Results, $expectedResults) -and
                $Key -ceq $expectedKey -and
                $Command -ceq 'ConvertFrom-StarsectorCsv' -and
                $Parameters.Path -ceq $expectedPath -and
                $Parameters.ConvertsCurlyQuotes -eq $true
        }
    }

    It 'reads only the replacing source''s copy, and returns it with the spreadsheet''s path' {

        # ARRANGE
        foreach ($owner in 'alpha', 'beta', 'core') {
            $script:CopyRowsByOwner[$owner] = New-CopyRows -Keys @("$owner-row") -Ids @("$owner-row")
        }

        $sources = @(
            (New-Source -Owner 'alpha' -HasSpreadsheet)
            (New-Source -Owner 'beta' -HasSpreadsheet)
            (New-Source -Owner 'core' -HasSpreadsheet)
        )
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv'
        $script:ReplacingSource = $sources[1]

        Mock Find-ReplacingSource { $script:ReplacingSource }

        $expectedAdded = @('beta-row=beta')
        $expectedReplacedPath = 'data/sheet.csv'
        $expectedReplacingOwner = 'beta'

        # ACT
        $actualRead = Read-Spreadsheet -Sources $sources -Spreadsheet $spreadsheet

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.Replacement.Path | Should -Be $expectedReplacedPath
        $actualRead.Replacement.Owner | Should -Be $expectedReplacingOwner
    }

    # The game compares a replaced spreadsheet's path as written, unlike a replaced JSON file's.
    It 'asks for the replacing source among all sources, with the path as written' {

        # ARRANGE
        $sources = @((New-Source -Owner 'alpha'), (New-Source -Owner 'core'))
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv'

        $expectedPath = 'data/sheet.csv'
        $expectedOwners = 'alpha core'

        # ACT
        Read-Spreadsheet -Sources $sources -Spreadsheet $spreadsheet | Out-Null

        # ASSERT
        Should -Invoke Find-ReplacingSource -Times 1 -Exactly -ParameterFilter {
            $Path -ceq $expectedPath -and "$($Sources.Owner)" -ceq $expectedOwners -and -not $AcceptsEitherSlash
        }
    }

    # The first copy's row of a key decides it: a later copy's row for that key defines nothing, even with an ID,
    # and a first row defining nothing still claims its key.
    It 'defines an ID by the first row of its key in search order' {

        # ARRANGE
        $script:CopyRowsByOwner['alpha'] = New-CopyRows -Keys @('claimed', 'shared') -Ids @('', 'shared_id')
        $script:CopyRowsByOwner['core'] = New-CopyRows -Keys @('claimed', 'shared', 'core_key') `
            -Ids @('late_id', 'shared_id', 'core_id')

        $sources = @((New-Source -Owner 'alpha' -HasSpreadsheet), (New-Source -Owner 'core' -HasSpreadsheet))
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv'

        $expectedAdded = @('shared_id=alpha', 'shared_id=core', 'core_id=core')

        # ACT
        Read-Spreadsheet -Sources $sources -Spreadsheet $spreadsheet | Out-Null

        # ASSERT
        $script:Added | Should -Be $expectedAdded
    }

    It 'skips a copy whose rows the game rejects, or that does not parse, and reads the others' {

        # ARRANGE
        Mock Invoke-CachedRead {

            $owner = Split-Path -Leaf ($Parameters.Path -replace '[\\/]data[\\/]sheet\.csv$', '')

            if ($owner -eq 'beta') {
                throw 'has a quote left open, at character 3 after the header'
            }

            $owner
        }

        Mock ConvertTo-SpreadsheetRowIds {

            if ("$Rows" -eq 'alpha') {
                throw 'has no effect column in every row'
            }

            $script:CopyRowsByOwner["$Rows"]
        }

        $script:CopyRowsByOwner['core'] = New-CopyRows -Keys @('c') -Ids @('y')

        $sources = @(
            (New-Source -Owner 'alpha' -HasSpreadsheet)
            (New-Source -Owner 'beta' -HasSpreadsheet)
            (New-Source -Owner 'core' -HasSpreadsheet)
        )
        $spreadsheet = New-SpreadsheetSource -Path 'data/sheet.csv' -IdColumn 'effect'

        $expectedAdded = @('y=core')
        $expectedSkippedPaths = @(
            "$($sources[0].Folder)\data\sheet.csv"
            "$($sources[1].Folder)\data\sheet.csv"
        )
        $expectedSkippedReasons = @(
            'has no effect column in every row'
            'has a quote left open, at character 3 after the header'
        )

        # ACT
        $actualRead = Read-Spreadsheet -Sources $sources -Spreadsheet $spreadsheet

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.SkippedFiles.Path | Should -Be $expectedSkippedPaths
        $actualRead.SkippedFiles.Reason | Should -Be $expectedSkippedReasons
    }
}
