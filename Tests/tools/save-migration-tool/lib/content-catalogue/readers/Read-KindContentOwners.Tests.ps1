BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\install\Get-CoreOwnerName.ps1"
    . "$libRoot\content-catalogue\owner-index\Add-ContentOwner.ps1"
    . "$libRoot\content-catalogue\owner-index\Merge-ContentOwners.ps1"
    . "$libRoot\content-catalogue\owner-index\New-ContentOwnerIndex.ps1"
    . "$libRoot\content-catalogue\readers\Read-ContentFileOwners.ps1"
    . "$libRoot\content-catalogue\readers\Read-KeyedFileOwners.ps1"
    . "$libRoot\content-catalogue\readers\Read-KindContentOwners.ps1"
    . "$libRoot\content-catalogue\readers\Read-SpreadsheetOwners.ps1"
    . "$libRoot\content-catalogue\readers\Select-ContentWithRow.ps1"

    function script:New-Part {
        # One part, its sources named for what the readers below are faked by.
        param(
            [string[]] $FileFolders = @(),
            [string[]] $SpreadsheetPaths = @(),
            [string[]] $KeyedPaths = @(),
            [string[]] $BuiltInIds = @()
        )

        return [pscustomobject]@{
            FileSets     = @($FileFolders | ForEach-Object { [pscustomobject]@{ Folder = $_ } })
            Spreadsheets = @($SpreadsheetPaths | ForEach-Object { [pscustomobject]@{ Path = $_ } })
            KeyedFiles   = @($KeyedPaths | ForEach-Object { [pscustomobject]@{ Path = $_ } })
            BuiltInIds   = $BuiltInIds
        }
    }

    function script:Read-Kind {
        # Reads a kind of the given parts.
        param([object[]] $Parts)

        $definition = [pscustomobject]@{ Kind = 'K'; Parts = $Parts }

        return Read-KindContentOwners -Sources @('sources') -Definition $definition -Cache $script:Cache
    }
}

Describe 'Read-KindContentOwners' {

    BeforeEach {

        $script:Added = [System.Collections.Generic.List[string]]::new()
        $script:Cache = [pscustomobject]@{ Name = 'the run''s cache' }

        Mock Get-CoreOwnerName { 'core' }

        # Each file set defines one ID named for it, reads 10 copies, and skips one.
        # The replacer replaces one file of each file set, and each spreadsheet or keyed file named "replaced".
        Mock Read-ContentFileOwners {
            [pscustomobject]@{
                OwnersById   = New-OwnerIndex @{ "$($FileSet.Folder)-id" = @('m') }
                CopyCount    = 10
                SkippedFiles = @([pscustomobject]@{ Path = "$($FileSet.Folder)\bad"; Reason = 'broken' })
                Replacements = @([pscustomobject]@{ Path = "$($FileSet.Folder)/a.ship"; Owner = 'replacer' })
            }
        }

        Mock Read-SpreadsheetOwners {

            $replacement = if ($Spreadsheet.Path -eq 'replaced.csv') {
                [pscustomobject]@{ Path = $Spreadsheet.Path; Owner = 'replacer' }
            } else {
                $null
            }

            [pscustomobject]@{
                OwnersById   = New-OwnerIndex @{ "$($Spreadsheet.Path)-id" = @('r') }
                SkippedFiles = @()
                Replacement  = $replacement
            }
        }

        Mock Read-KeyedFileOwners {

            $replacement = if ($KeyedFile.Path -eq 'replaced.json') {
                [pscustomobject]@{ Path = $KeyedFile.Path; Owner = 'replacer' }
            } else {
                $null
            }

            [pscustomobject]@{
                OwnersById   = New-OwnerIndex @{ "$($KeyedFile.Path)-id" = @('j') }
                SkippedFiles = @([pscustomobject]@{ Path = "$($KeyedFile.Path)\bad"; Reason = 'broken' })
                Replacement  = $replacement
            }
        }

        # Each fake names its inputs in the one ID it returns, so a case reads back how the index was built.
        Mock Select-ContentWithRow {
            New-OwnerIndex @{ "selected-$(@($FileOwners.Keys) -join '+')-$(@($RowOwners.Keys) -join '+')" = @('s') }
        }
        Mock Merge-ContentOwners {
            New-OwnerIndex @{ "merged-$(@($Into.Keys) -join '+')-$(@($From.Keys) -join '+')" = @('x') }
        }
        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }
    }

    # The game defines a hull or weapon only when both its file and its row exist.
    It 'keeps only the IDs a part''s files and the rows of any of its spreadsheets both define' {

        # ARRANGE
        $parts = @(New-Part -FileFolders 'files' -SpreadsheetPaths 'one.csv', 'two.csv')

        $expectedOwners = 'merged--selected-merged--files-id-merged-merged--one.csv-id-two.csv-id=x'

        # ACT
        $actualRead = Read-Kind -Parts $parts

        # ASSERT
        Format-OwnerIndex -OwnersById $actualRead.OwnersById | Should -Be $expectedOwners
        Should -Invoke Read-SpreadsheetOwners -ParameterFilter { "$Sources" -eq 'sources' } -Times 2 -Exactly
    }

    It 'takes the files of every set, or the rows of every spreadsheet, of a part with only one of them' -ForEach @(
        @{ Part = @{ FileFolders = @('a', 'b') }; Owners = 'merged--merged-merged-merged--a-id-b-id-=x' }
        @{ Part = @{ SpreadsheetPaths = @('one.csv') }; Owners = 'merged--merged--merged--one.csv-id=x' }
    ) {

        # ARRANGE
        $parts = @(New-Part @Part)

        $expectedOwners = $Owners

        # ACT
        $actualRead = Read-Kind -Parts $parts

        # ASSERT
        Format-OwnerIndex -OwnersById $actualRead.OwnersById | Should -Be $expectedOwners
        Should -Invoke Select-ContentWithRow -Times 0 -Exactly
    }

    It 'adds the IDs of a part''s keyed files' {

        # ARRANGE
        $parts = @(New-Part -KeyedPaths 'planets.json')

        $expectedOwners = 'merged--merged-merged---planets.json-id=x'

        # ACT
        $actualRead = Read-Kind -Parts $parts

        # ASSERT
        Format-OwnerIndex -OwnersById $actualRead.OwnersById | Should -Be $expectedOwners
        Should -Invoke Read-KeyedFileOwners -ParameterFilter { "$Sources" -eq 'sources' } -Times 1 -Exactly
    }

    # Spreadsheets and keyed files several kinds read are parsed once, through the one cache of the run.
    It 'hands the run''s cache to every reader' {

        # ARRANGE
        $parts = @(New-Part -FileFolders 'files' -SpreadsheetPaths 'one.csv' -KeyedPaths 'planets.json')

        $expectedCache = $script:Cache

        # ACT
        Read-Kind -Parts $parts | Out-Null

        # ASSERT
        Should `
            -Invoke Read-ContentFileOwners `
            -ParameterFilter { [object]::ReferenceEquals($Cache, $expectedCache) } `
            -Times 1 `
            -Exactly

        Should `
            -Invoke Read-SpreadsheetOwners `
            -ParameterFilter { [object]::ReferenceEquals($Cache, $expectedCache) } `
            -Times 1 `
            -Exactly

        Should `
            -Invoke Read-KeyedFileOwners `
            -ParameterFilter { [object]::ReferenceEquals($Cache, $expectedCache) } `
            -Times 1 `
            -Exactly
    }

    # The game builds some variants in its own code, with no file to find them in.
    It 'adds the game''s built-in IDs, owned by the game' {

        # ARRANGE
        $parts = @(New-Part -BuiltInIds 'omen1', 'condor1')

        $expectedAdded = @('omen1=core', 'condor1=core')

        # ACT
        Read-Kind -Parts $parts | Out-Null

        # ASSERT
        $script:Added | Should -Be $expectedAdded
    }

    It 'sums the content file copies read, and returns what each source skipped and each file a mod replaces' {

        # ARRANGE
        $parts = @(
            New-Part -FileFolders 'files' -SpreadsheetPaths 'replaced.csv', 'kept.csv'
            New-Part -KeyedPaths 'replaced.json', 'kept.json'
        )

        $expectedContentFileCount = 10
        $expectedSkippedPaths = @('files\bad', 'replaced.json\bad', 'kept.json\bad')
        $expectedReplacedPaths = @('files/a.ship', 'replaced.csv', 'replaced.json')
        $expectedReplacingOwners = @('replacer', 'replacer', 'replacer')

        # ACT
        $actualRead = Read-Kind -Parts $parts

        # ASSERT
        $actualRead.ContentFileCount | Should -Be $expectedContentFileCount
        $actualRead.SkippedFiles.Path | Should -Be $expectedSkippedPaths
        $actualRead.Replacements.Path | Should -Be $expectedReplacedPaths
        $actualRead.Replacements.Owner | Should -Be $expectedReplacingOwners
    }
}
