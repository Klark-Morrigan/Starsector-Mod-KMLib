BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Format-ModVersion.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Get-ModIdComparer.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Group-ModCandidate.ps1"

    function script:New-Candidate {
        # One installed copy of a mod, as Read-ModCandidate reads it, in a folder of the given name.
        param(
            [string] $Id,
            [string] $SortKey,
            [string] $Folder,
            [string] $Version = '1.0'
        )

        return [pscustomobject]@{
            Id      = $Id
            SortKey = $SortKey
            Version = $Version
            Folder  = [System.IO.DirectoryInfo]::new("C:\Games\mods\$Folder")
        }
    }
}

Describe 'Group-ModCandidate' {

    BeforeEach {
        Mock Format-ModVersion { "$Version" }
        Mock Get-ModIdComparer { [System.StringComparer]::Ordinal }
    }

    It 'picks the highest sort key per ID as the winner, the rest highest first as superseded' {

        # ARRANGE
        $candidates = @(
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'old')
            (New-Candidate -Id 'alpha' -SortKey '000003' -Folder 'new')
            (New-Candidate -Id 'alpha' -SortKey '000002' -Folder 'middle')
            (New-Candidate -Id 'beta' -SortKey '000001' -Folder 'only')
        )
        $expectedIds = @('alpha', 'beta')
        $expectedWinner = 'new'
        $expectedSuperseded = @('middle', 'old')
        $expectedLoneSupersededCount = 0

        # ACT
        $actualGroups = @(Group-ModCandidate -Candidates $candidates)

        # ASSERT
        $actualGroups.Id | Should -Be $expectedIds
        $actualGroups[0].Winner.Folder.Name | Should -Be $expectedWinner
        $actualGroups[0].Superseded.Folder.Name | Should -Be $expectedSuperseded
        $actualGroups[1].Superseded.Count | Should -Be $expectedLoneSupersededCount
    }

    It 'orders copies whose version numbers tie by the version as written, highest first' {

        # ARRANGE
        $candidates = @(
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'Alpha copy' -Version '1.5a')
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'Zeta copy' -Version '1.5b')
        )
        $expectedWinner = 'Zeta copy'

        # ACT
        $actualGroups = @(Group-ModCandidate -Candidates $candidates)

        # ASSERT
        $actualGroups[0].Winner.Folder.Name | Should -Be $expectedWinner
    }

    It 'orders copies at the very same version by folder name, whatever order they came in' {

        # ARRANGE
        $candidates = @(
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'Zeta copy')
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'Alpha copy')
        )
        $expectedWinner = 'Alpha copy'
        $expectedSuperseded = @('Zeta copy')

        # ACT
        $actualGroups = @(Group-ModCandidate -Candidates $candidates)

        # ASSERT
        $actualGroups[0].Winner.Folder.Name | Should -Be $expectedWinner
        $actualGroups[0].Superseded.Folder.Name | Should -Be $expectedSuperseded
    }

    # Two copies whose IDs differ only in case are two mods to the game, each loading its own winner.
    It 'keeps copies the mod ID comparer tells apart in separate groups' {

        # ARRANGE
        $candidates = @(
            (New-Candidate -Id 'Alpha' -SortKey '000002' -Folder 'upper')
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'lower')
        )
        $expectedIds = @('alpha', 'Alpha')
        $expectedWinners = @('lower', 'upper')
        $expectedSupersededCount = 0

        # ACT
        $actualGroups = @(Group-ModCandidate -Candidates $candidates)

        # ASSERT
        $actualGroups.Id | Should -BeExactly $expectedIds
        $actualGroups.Winner.Folder.Name | Should -Be $expectedWinners
        $actualGroups[0].Superseded.Count | Should -Be $expectedSupersededCount
        $actualGroups[1].Superseded.Count | Should -Be $expectedSupersededCount
    }

    It 'returns nothing for no candidates' {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualGroups = @(Group-ModCandidate -Candidates @())

        # ASSERT
        $actualGroups.Count | Should -Be $expectedCount
    }
}
