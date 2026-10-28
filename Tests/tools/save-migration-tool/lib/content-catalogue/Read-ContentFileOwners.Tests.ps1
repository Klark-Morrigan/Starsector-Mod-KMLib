BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue"

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\Add-ContentOwner.ps1"
    . "$libRoot\Find-ContentFiles.ps1"
    . "$libRoot\Find-ListedContentFiles.ps1"
    . "$libRoot\New-ContentOwnerIndex.ps1"
    . "$libRoot\Read-ContentFileId.ps1"
    . "$libRoot\Read-ContentFileOwners.ps1"
    . "$libRoot\Read-SpreadsheetOwners.ps1"
    . "$libRoot\Select-MergedCopies.ps1"

    function script:New-File {
        # One file of Find-ContentFiles, with a copy per owner; each copy's path is <owner>\<name>.
        param(
            [string] $Name,
            [string[]] $Owners
        )

        $copies = @(foreach ($owner in $Owners) {
            [pscustomobject]@{ Source = [pscustomobject]@{ Owner = $owner }; Path = "$owner\$Name" }
        })

        return [pscustomobject]@{ RelativePath = "data/hulls/$Name"; Copies = $copies }
    }
}

Describe 'Read-ContentFileOwners' {

    BeforeEach {

        $script:FileSet = [pscustomobject]@{
            Folder   = 'data/hulls'
            Ending   = 'ship'
            IdKey    = 'hullId'
            ListedBy = $null
        }
        $script:Added = [System.Collections.Generic.List[string]]::new()

        # Each copy's ID is its file name, unless the case says otherwise.
        Mock Read-ContentFileId { [System.IO.Path]::GetFileNameWithoutExtension($Path) }
        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }

        # Merge order is the copies' order reversed, so a case sees whether it was asked.
        Mock Select-MergedCopies { [object[]] $File.Copies[($File.Copies.Count - 1)..0] }
    }

    It 'records the ID each one-copy file sets, with the copy''s owner, reading the set''s key' {

        # ARRANGE
        Mock Find-ContentFiles { (New-File -Name 'a.ship' -Owners 'alpha'), (New-File -Name 'b.ship' -Owners 'core') }

        $expectedAdded = @('a=alpha', 'b=core')
        $expectedFileCount = 2
        $expectedSpreadsheetCount = 0
        $expectedSkippedCount = 0

        # ACT
        $actualRead = Read-ContentFileOwners -Sources @('sources') -FileSet $script:FileSet

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.FileCount | Should -Be $expectedFileCount
        $actualRead.SpreadsheetCount | Should -Be $expectedSpreadsheetCount
        $actualRead.SkippedFiles.Count | Should -Be $expectedSkippedCount
        Should -Invoke Find-ContentFiles -ParameterFilter { "$Sources" -eq 'sources' -and $FileSet.IdKey -eq 'hullId' }
        Should -Invoke Read-ContentFileId -ParameterFilter { $IdKey -eq 'hullId' } -Times 2 -Exactly
        Should -Invoke Select-MergedCopies -Times 0 -Exactly
    }

    # factions.csv lists every .faction file the game reads.
    It 'reads the files a spreadsheet lists, counting the spreadsheet and returning what it skipped' {

        # ARRANGE
        $listing = [pscustomobject]@{ Path = 'data/world/factions/factions.csv' }
        $script:FileSet = [pscustomobject]@{ IdKey = 'id'; ListedBy = $listing }

        Mock Read-SpreadsheetOwners {
            [pscustomobject]@{
                OwnersById   = New-OwnerIndex @{ 'data/world/factions/x.faction' = @('core') }
                FileCount    = 3
                SkippedFiles = @([pscustomobject]@{ Path = 'alpha\factions.csv'; Reason = 'broken' })
            }
        }
        Mock Find-ListedContentFiles { New-File -Name 'x.faction' -Owners 'alpha' }
        Mock Find-ContentFiles { }

        $expectedAdded = @('x=alpha')
        $expectedSpreadsheetCount = 3
        $expectedSkippedPaths = @('alpha\factions.csv')

        # ACT
        $actualRead = Read-ContentFileOwners -Sources @('sources') -FileSet $script:FileSet

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.SpreadsheetCount | Should -Be $expectedSpreadsheetCount
        $actualRead.SkippedFiles.Path | Should -Be $expectedSkippedPaths
        Should -Invoke Read-SpreadsheetOwners -ParameterFilter {
            "$Sources" -eq 'sources' -and [object]::ReferenceEquals($Spreadsheet, $listing)
        } -Times 1 -Exactly
        Should -Invoke Find-ListedContentFiles -ParameterFilter {
            "$RelativePaths" -eq 'data/world/factions/x.faction'
        } -Times 1 -Exactly
        Should -Invoke Find-ContentFiles -Times 0 -Exactly
    }

    # Merge order is core, beta, alpha; beta only patches.
    It 'gives a file of several copies the ID of the last copy in merge order that sets one' {

        # ARRANGE
        Mock Find-ContentFiles { New-File -Name 'a.ship' -Owners 'alpha', 'beta', 'core' }
        Mock Read-ContentFileId {

            switch ($Path) {
                'alpha\a.ship' { 'from_alpha' }
                'beta\a.ship' { $null }
                'core\a.ship' { 'from_core' }
            }
        }

        $expectedAdded = @('from_alpha=alpha')
        $expectedFileCount = 3

        # ACT
        $actualRead = Read-ContentFileOwners -Sources @() -FileSet $script:FileSet

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.FileCount | Should -Be $expectedFileCount
    }

    # Mods often restate the game's own file in full; removing one of them leaves the ID defined.
    # Merge order is core, renamer, restater: the restater's ID wins, and the core's matches it.
    It 'owns the ID by every copy that sets the ID the merge ends with, and by no copy setting another' {

        # ARRANGE
        Mock Find-ContentFiles { New-File -Name 'a.ship' -Owners 'restater', 'renamer', 'core' }
        Mock Read-ContentFileId {

            switch ($Path) {
                'restater\a.ship' { 'kept' }
                'renamer\a.ship' { 'renamed' }
                'core\a.ship' { 'kept' }
            }
        }

        $expectedAdded = @('kept=core', 'kept=restater')

        # ACT
        Read-ContentFileOwners -Sources @() -FileSet $script:FileSet | Out-Null

        # ASSERT
        $script:Added | Should -Be $expectedAdded
    }

    It 'skips a copy that does not parse, keeping the ID the others set, and returns it with the reason' {

        # ARRANGE
        Mock Find-ContentFiles { New-File -Name 'a.ship' -Owners 'alpha', 'core' }
        Mock Read-ContentFileId {

            if ($Path -eq 'alpha\a.ship') {
                throw 'not valid JSON'
            }

            'from_core'
        }

        $expectedAdded = @('from_core=core')
        $expectedSkippedPaths = @('alpha\a.ship')
        $expectedSkippedReasons = @('not valid JSON')

        # ACT
        $actualRead = Read-ContentFileOwners -Sources @() -FileSet $script:FileSet

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.SkippedFiles.Path | Should -Be $expectedSkippedPaths
        $actualRead.SkippedFiles.Reason | Should -Be $expectedSkippedReasons
    }

    It 'records nothing for a file whose copies set no ID, or an empty one' {

        # ARRANGE
        Mock Find-ContentFiles {
            (New-File -Name 'patch.ship' -Owners 'alpha'), (New-File -Name 'empty.ship' -Owners 'beta')
        }
        Mock Read-ContentFileId {

            if ($Path -eq 'beta\empty.ship') {
                return ''
            }

            $null
        }

        $expectedAddedCount = 0

        # ACT
        Read-ContentFileOwners -Sources @() -FileSet $script:FileSet | Out-Null

        # ASSERT
        $script:Added.Count | Should -Be $expectedAddedCount
    }
}
