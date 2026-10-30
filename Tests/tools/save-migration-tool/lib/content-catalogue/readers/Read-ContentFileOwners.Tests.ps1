BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue"

    . "$PSScriptRoot\..\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\owner-index\Add-ContentOwner.ps1"
    . "$libRoot\owner-index\New-ContentOwnerIndex.ps1"
    . "$libRoot\readers\Find-ContentFiles.ps1"
    . "$libRoot\readers\Find-ListedContentFiles.ps1"
    . "$libRoot\readers\Read-ContentFileId.ps1"
    . "$libRoot\readers\Read-ContentFileOwners.ps1"
    . "$libRoot\readers\Read-SpreadsheetOwners.ps1"
    . "$libRoot\readers\Select-MergedCopies.ps1"

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

    function script:Read-FileSet {
        # Reads the case's file set across the given sources with the case's cache.
        param([object[]] $Sources = @())

        return Read-ContentFileOwners -Sources $Sources -FileSet $script:FileSet -Cache $script:Cache
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
        $script:Cache = [pscustomobject]@{ SpreadsheetsByKey = 'the run''s spreadsheets' }

        # Each copy's ID is its file name, unless the case says otherwise.
        Mock Read-ContentFileId { [System.IO.Path]::GetFileNameWithoutExtension($Path) }
        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }

        # Merge order is the copies' order reversed, so a case sees whether it was asked.
        Mock Select-MergedCopies {
            [pscustomobject]@{ Copies = [object[]] $File.Copies[($File.Copies.Count - 1)..0]; Replacement = $null }
        }
    }

    It 'records the ID each one-copy file sets, with the copy''s owner, reading the set''s key' {

        # ARRANGE
        Mock Find-ContentFiles { (New-File -Name 'a.ship' -Owners 'alpha'), (New-File -Name 'b.ship' -Owners 'core') }

        $expectedAdded = @('a=alpha', 'b=core')
        $expectedCopyCount = 2
        $expectedSkippedCount = 0
        $expectedReplacementCount = 0

        # ACT
        $actualRead = Read-FileSet -Sources @('sources')

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.CopyCount | Should -Be $expectedCopyCount
        $actualRead.SkippedFiles.Count | Should -Be $expectedSkippedCount
        $actualRead.Replacements.Count | Should -Be $expectedReplacementCount
        Should -Invoke Find-ContentFiles -ParameterFilter { "$Sources" -eq 'sources' -and $FileSet.IdKey -eq 'hullId' }
        Should -Invoke Read-ContentFileId -ParameterFilter { $IdKey -eq 'hullId' } -Times 2 -Exactly
        Should -Invoke Select-MergedCopies -Times 0 -Exactly
    }

    # factions.csv lists every .faction file the game reads.
    It 'reads the files a spreadsheet lists, with the run''s cache, returning what it skipped and replaced' {

        # ARRANGE
        $listing = [pscustomobject]@{ Path = 'data/world/factions/factions.csv' }
        $script:FileSet = [pscustomobject]@{ IdKey = 'id'; ListedBy = $listing }

        Mock Read-SpreadsheetOwners {
            [pscustomobject]@{
                OwnersById   = New-OwnerIndex @{ 'data/world/factions/x.faction' = @('core') }
                SkippedFiles = @([pscustomobject]@{ Path = 'alpha\factions.csv'; Reason = 'broken' })
                Replacement  = [pscustomobject]@{ Path = 'data/world/factions/factions.csv'; Owner = 'alpha' }
            }
        }
        Mock Find-ListedContentFiles { New-File -Name 'x.faction' -Owners 'alpha' }
        Mock Find-ContentFiles { }

        $expectedAdded = @('x=alpha')
        $expectedSkippedPaths = @('alpha\factions.csv')
        $expectedReplacedPaths = @('data/world/factions/factions.csv')
        $expectedCache = $script:Cache

        # ACT
        $actualRead = Read-FileSet -Sources @('sources')

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.SkippedFiles.Path | Should -Be $expectedSkippedPaths
        $actualRead.Replacements.Path | Should -Be $expectedReplacedPaths
        Should -Invoke Read-SpreadsheetOwners -ParameterFilter {
            "$Sources" -eq 'sources' -and
                [object]::ReferenceEquals($Spreadsheet, $listing) -and
                [object]::ReferenceEquals($Cache, $expectedCache)
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
        $expectedCopyCount = 3

        # ACT
        $actualRead = Read-FileSet

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.CopyCount | Should -Be $expectedCopyCount
    }

    # A mod replacing the file leaves only its copy, so the merge hands back that copy alone.
    It 'reads only the copies the merge keeps, and returns each file a mod replaces with that mod' {

        # ARRANGE
        Mock Find-ContentFiles {
            (New-File -Name 'a.ship' -Owners 'alpha', 'core'), (New-File -Name 'b.ship' -Owners 'beta', 'core')
        }
        Mock Select-MergedCopies {

            if ($File.RelativePath -ne 'data/hulls/a.ship') {
                return [pscustomobject]@{ Copies = $File.Copies; Replacement = $null }
            }

            [pscustomobject]@{
                Copies      = @($File.Copies[0])
                Replacement = [pscustomobject]@{ Path = $File.RelativePath; Owner = 'alpha' }
            }
        }

        $expectedAdded = @('a=alpha', 'b=beta', 'b=core')
        $expectedCopyCount = 3
        $expectedReplacedPaths = @('data/hulls/a.ship')
        $expectedReplacingOwners = @('alpha')

        # ACT
        $actualRead = Read-FileSet

        # ASSERT
        $script:Added | Should -Be $expectedAdded
        $actualRead.CopyCount | Should -Be $expectedCopyCount
        $actualRead.Replacements.Path | Should -Be $expectedReplacedPaths
        $actualRead.Replacements.Owner | Should -Be $expectedReplacingOwners
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
        Read-FileSet | Out-Null

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
        $actualRead = Read-FileSet

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
        Read-FileSet | Out-Null

        # ASSERT
        $script:Added.Count | Should -Be $expectedAddedCount
    }
}
