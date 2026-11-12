BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\catalogue-cache\Get-ContentKindInputFiles.ps1"
    . "$libRoot\content-catalogue\readers\Find-ContentFiles.ps1"
    . "$libRoot\content-catalogue\readers\Find-ListedContentFiles.ps1"
    . "$libRoot\content-catalogue\readers\Read-SpreadsheetOwners.ps1"

    function script:New-File {
        # One file and its copies, as Find-ContentFiles and Find-ListedContentFiles return them.
        param([string[]] $Paths)

        return [pscustomobject]@{

            RelativePath = 'data/x'
            Copies = @(foreach ($path in $Paths) {

                [pscustomobject]@{
                    Source = 's';
                    Path = $path }
            })
        }
    }

    function script:New-Part {
        # One part of a definition, as New-ContentPart describes it.
        param(
            [object[]] $FileSets = @(),
            [object[]] $Spreadsheets = @(),
            [object[]] $KeyedFiles = @())

        return [pscustomobject]@{
            FileSets = $FileSets;
            Spreadsheets = $Spreadsheets;
            KeyedFiles = $KeyedFiles }
    }
}

Describe 'Get-ContentKindInputFiles' {

    BeforeEach {

        Mock Find-ContentFiles {
            New-File @(
                "C:\core\$($FileSet.Folder)\b.ship",
                "C:\mods\a\$($FileSet.Folder)\b.ship")
        }

        Mock Find-ListedContentFiles {
            foreach ($relativePath in $RelativePaths) {
                New-File @("C:\core\$relativePath")
            }
        }

        Mock Read-SpreadsheetOwners {
            [pscustomobject]@{
                OwnersById = New-OwnerIndex @{
                    'listed/one.faction' = @('m') }
        } }
    }

    It 'lists every copy of a folder''s files, a spreadsheet and a keyed file, sorted, each once' {

        # ARRANGE
        $definition = [pscustomobject]@{

            Parts = @(
                New-Part `
                    -FileSets @([pscustomobject]@{ Folder = 'hulls'; ListedBy = $null }) `
                    -Spreadsheets @([pscustomobject]@{ Path = 'hulls/ship_data.csv' })
                New-Part `
                    -FileSets @([pscustomobject]@{ Folder = 'hulls'; ListedBy = $null }) `
                    -KeyedFiles @([pscustomobject]@{ Path = 'config/planets.json' })
            )
        }

        $expectedPaths = @(
            'C:\core\config/planets.json'
            'C:\core\hulls/ship_data.csv'
            'C:\core\hulls\b.ship'
            'C:\mods\a\hulls\b.ship'
        )

        # ACT
        $actualPaths = @(Get-ContentKindInputFiles `
            -Sources @('sources') `
            -Definition $definition `
            -Cache 'cache')

        # ASSERT
        $actualPaths | Should -Be $expectedPaths

        Should -Invoke Find-ContentFiles `
            -ParameterFilter { "$Sources" -eq 'sources' } `
            -Times 2 `
            -Exactly
    }

    It 'lists a listing spreadsheet''s copies and the copies of every file it lists, read through the run''s cache' {

        # ARRANGE
        $listing = [pscustomobject]@{ Path = 'factions/factions.csv' }
        $definition = [pscustomobject]@{
            Parts = @(New-Part -FileSets @([pscustomobject]@{
                Folder = $null;
                ListedBy = $listing }))
        }

        $expectedPaths = @(
            'C:\core\factions/factions.csv',
            'C:\core\listed/one.faction')

        # ACT
        $actualPaths = @(Get-ContentKindInputFiles `
            -Sources @('sources') `
            -Definition $definition `
            -Cache 'cache')

        # ASSERT
        $actualPaths | Should -Be $expectedPaths

        Should -Invoke Read-SpreadsheetOwners `
            -ParameterFilter {
                "$Sources" -eq 'sources' `
                    -and $Spreadsheet.Path -eq 'factions/factions.csv' `
                    -and $Cache -eq 'cache'
            } `
            -Times 1 `
            -Exactly

        Should -Invoke Find-ContentFiles `
            -Times 0 `
            -Exactly
    }

    It 'lists nothing for a kind the game builds in its own code' {

        # ARRANGE
        $definition = [pscustomobject]@{ Parts = @(New-Part) }

        # ACT
        $actualPaths = @(Get-ContentKindInputFiles `
            -Sources @('sources') `
            -Definition $definition `
            -Cache 'cache')

        # ASSERT
        $actualPaths | Should -BeNullOrEmpty
    }
}
