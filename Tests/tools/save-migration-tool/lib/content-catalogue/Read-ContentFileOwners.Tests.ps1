BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue"

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\Add-ContentOwner.ps1"
    . "$libRoot\Find-ContentFiles.ps1"
    . "$libRoot\Find-ListedContentFiles.ps1"
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

        Mock Find-ContentFiles { (New-File -Name 'a.ship' -Owners 'alpha'), (New-File -Name 'b.ship' -Owners 'core') }

        $read = Read-ContentFileOwners -Sources @('sources') -FileSet $script:FileSet

        $script:Added | Should -Be @('a=alpha', 'b=core')
        $read.FileCount | Should -Be 2
        $read.SpreadsheetCount | Should -Be 0
        $read.SkippedFiles.Count | Should -Be 0

        Should -Invoke Find-ContentFiles -ParameterFilter { "$Sources" -eq 'sources' -and $FileSet.IdKey -eq 'hullId' }
        Should -Invoke Read-ContentFileId -ParameterFilter { $IdKey -eq 'hullId' } -Times 2 -Exactly
        Should -Invoke Select-MergedCopies -Times 0 -Exactly
    }

    # factions.csv lists every .faction file the game reads.
    It 'reads the files a spreadsheet lists, counting the spreadsheet and returning what it skipped' {

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

        $read = Read-ContentFileOwners -Sources @('sources') -FileSet $script:FileSet

        $script:Added | Should -Be @('x=alpha')
        $read.SpreadsheetCount | Should -Be 3
        $read.SkippedFiles.Path | Should -Be @('alpha\factions.csv')

        Should -Invoke Read-SpreadsheetOwners -ParameterFilter {
            "$Sources" -eq 'sources' -and [object]::ReferenceEquals($Spreadsheet, $listing)
        } -Times 1 -Exactly
        Should -Invoke Find-ListedContentFiles -ParameterFilter {
            "$RelativePaths" -eq 'data/world/factions/x.faction'
        } -Times 1 -Exactly
        Should -Invoke Find-ContentFiles -Times 0 -Exactly
    }

    It 'gives a file of several copies the ID of the last copy in merge order that sets one' {

        Mock Find-ContentFiles { New-File -Name 'a.ship' -Owners 'alpha', 'beta', 'core' }
        Mock Read-ContentFileId {

            switch ($Path) {
                'alpha\a.ship' { 'from_alpha' }
                'beta\a.ship' { $null }
                'core\a.ship' { 'from_core' }
            }
        }

        $read = Read-ContentFileOwners -Sources @() -FileSet $script:FileSet

        # Merge order is core, beta, alpha; beta only patches.
        $script:Added | Should -Be @('from_alpha=alpha')
        $read.FileCount | Should -Be 3
    }

    # Mods often restate the game's own file in full; removing one of them leaves the ID defined.
    It 'owns the ID by every copy that sets the ID the merge ends with, and by no copy setting another' {

        Mock Find-ContentFiles { New-File -Name 'a.ship' -Owners 'restater', 'renamer', 'core' }
        Mock Read-ContentFileId {

            switch ($Path) {
                'restater\a.ship' { 'kept' }
                'renamer\a.ship' { 'renamed' }
                'core\a.ship' { 'kept' }
            }
        }

        Read-ContentFileOwners -Sources @() -FileSet $script:FileSet | Out-Null

        # Merge order is core, renamer, restater: the restater's ID wins, and the core's matches it.
        $script:Added | Should -Be @('kept=core', 'kept=restater')
    }

    It 'skips a copy that does not parse, keeping the ID the others set, and returns it with the reason' {

        Mock Find-ContentFiles { New-File -Name 'a.ship' -Owners 'alpha', 'core' }
        Mock Read-ContentFileId {

            if ($Path -eq 'alpha\a.ship') {
                throw 'not valid JSON'
            }

            'from_core'
        }

        $read = Read-ContentFileOwners -Sources @() -FileSet $script:FileSet

        $script:Added | Should -Be @('from_core=core')
        $read.SkippedFiles.Path | Should -Be @('alpha\a.ship')
        $read.SkippedFiles.Reason | Should -Be @('not valid JSON')
    }

    It 'records nothing for a file whose copies set no ID, or an empty one' {

        Mock Find-ContentFiles {
            (New-File -Name 'patch.ship' -Owners 'alpha'), (New-File -Name 'empty.ship' -Owners 'beta')
        }
        Mock Read-ContentFileId {

            if ($Path -eq 'beta\empty.ship') {
                return ''
            }

            $null
        }

        Read-ContentFileOwners -Sources @() -FileSet $script:FileSet | Out-Null

        $script:Added.Count | Should -Be 0
    }
}
