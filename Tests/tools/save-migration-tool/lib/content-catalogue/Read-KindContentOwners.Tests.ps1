BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\class-index\Get-CoreOwnerName.ps1"
    . "$libRoot\content-catalogue\Add-ContentOwner.ps1"
    . "$libRoot\content-catalogue\Merge-ContentOwners.ps1"
    . "$libRoot\content-catalogue\Read-ContentFileOwners.ps1"
    . "$libRoot\content-catalogue\Read-KeyedFileOwners.ps1"
    . "$libRoot\content-catalogue\Read-KindContentOwners.ps1"
    . "$libRoot\content-catalogue\Read-SpreadsheetOwners.ps1"
    . "$libRoot\content-catalogue\Select-ContentWithRow.ps1"

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

        return Read-KindContentOwners -Sources @('sources') -Definition $definition
    }
}

Describe 'Read-KindContentOwners' {

    BeforeEach {

        $script:Added = [System.Collections.Generic.List[string]]::new()

        Mock Get-CoreOwnerName { 'core' }

        # Each source defines one ID named for it, reads 10 files, and skips one.
        Mock Read-ContentFileOwners {
            [pscustomobject]@{
                OwnersById       = New-OwnerIndex @{ "$($FileSet.Folder)-id" = @('m') }
                FileCount        = 10
                SpreadsheetCount = 1
                SkippedFiles     = @([pscustomobject]@{ Path = "$($FileSet.Folder)\bad"; Reason = 'broken' })
            }
        }

        Mock Read-SpreadsheetOwners {

            $replacingOwner = if ($Spreadsheet.Path -eq 'replaced.csv') {
                'replacer'
            } else {
                $null
            }

            [pscustomobject]@{
                OwnersById     = New-OwnerIndex @{ "$($Spreadsheet.Path)-id" = @('r') }
                FileCount      = 3
                SkippedFiles   = @()
                ReplacingOwner = $replacingOwner
            }
        }

        Mock Read-KeyedFileOwners {
            [pscustomobject]@{
                OwnersById   = New-OwnerIndex @{ "$($KeyedFile.Path)-id" = @('j') }
                FileCount    = 2
                SkippedFiles = @([pscustomobject]@{ Path = "$($KeyedFile.Path)\bad"; Reason = 'broken' })
            }
        }

        Mock Select-ContentWithRow {
            New-OwnerIndex @{ "selected-$(@($FileOwners.Keys) -join '+')-$(@($RowOwners.Keys) -join '+')" = @('s') }
        }
        Mock Merge-ContentOwners {
            New-OwnerIndex @{ "merged-$(@($Into.Keys) -join '+')-$(@($From.Keys) -join '+')" = @('x') }
        }
        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }
    }

    It 'keeps only the IDs a part''s files and the rows of any of its spreadsheets both define' {

        $read = Read-Kind -Parts @(New-Part -FileFolders 'files' -SpreadsheetPaths 'one.csv', 'two.csv')

        Format-OwnerIndex -OwnersById $read.OwnersById |
            Should -Be 'merged--selected-merged--files-id-merged-merged--one.csv-id-two.csv-id=x'

        Should -Invoke Read-SpreadsheetOwners -ParameterFilter { "$Sources" -eq 'sources' } -Times 2 -Exactly
    }

    It 'takes the files of every set, or the rows of every spreadsheet, of a part that has only one of them' {

        $files = Read-Kind -Parts @(New-Part -FileFolders 'a', 'b')
        $rows = Read-Kind -Parts @(New-Part -SpreadsheetPaths 'one.csv')

        Format-OwnerIndex -OwnersById $files.OwnersById | Should -Be 'merged--merged-merged-merged--a-id-b-id-=x'
        Format-OwnerIndex -OwnersById $rows.OwnersById | Should -Be 'merged--merged--merged--one.csv-id=x'

        Should -Invoke Select-ContentWithRow -Times 0 -Exactly
    }

    It 'adds the IDs of a part''s keyed files' {

        $read = Read-Kind -Parts @(New-Part -KeyedPaths 'planets.json')

        Format-OwnerIndex -OwnersById $read.OwnersById | Should -Be 'merged--merged-merged---planets.json-id=x'

        Should -Invoke Read-KeyedFileOwners -ParameterFilter { "$Sources" -eq 'sources' } -Times 1 -Exactly
    }

    It 'adds the game''s built-in IDs, owned by the game' {

        Read-Kind -Parts @(New-Part -BuiltInIds 'omen1', 'condor1') | Out-Null

        $script:Added | Should -Be @('omen1=core', 'condor1=core')
    }

    It 'sums what every source read, and returns what each skipped and each spreadsheet a mod replaces' {

        $read = Read-Kind -Parts @(
            New-Part -FileFolders 'files' -SpreadsheetPaths 'replaced.csv'
            New-Part -KeyedPaths 'planets.json'
        )

        $read.ContentFileCount | Should -Be 12
        $read.SpreadsheetCount | Should -Be 4
        $read.SkippedFiles.Path | Should -Be @('files\bad', 'planets.json\bad')
        $read.Replacements.Spreadsheet | Should -Be @('replaced.csv')
        $read.Replacements.Owner | Should -Be @('replacer')
    }
}
