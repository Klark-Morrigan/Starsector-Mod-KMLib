BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Get-ModIdComparer.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Get-ModStatusNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Group-ModCandidate.ps1"

    function script:New-Candidate {
        # One installed copy of a mod, as Read-ModCandidate reads it, in a folder of the given name.
        param(
            [string] $Id,
            [string] $Folder,
            [string] $Status = 'P'
        )

        return [pscustomobject]@{
            Id     = $Id
            Status = $Status
            Folder = [System.IO.DirectoryInfo]::new("C:\Games\mods\$Folder")
        }
    }
}

Describe 'Group-ModCandidate' {

    BeforeEach {
        Mock Get-ModIdComparer { [System.StringComparer]::Ordinal }
        Mock Get-ModStatusNames { [pscustomobject]@{ Parsed = 'P'; Unparsed = 'U' } }
    }

    # The game keeps the first copy of an ID it reads and drops the rest, whatever their versions.
    It 'picks the first copy per ID as the winner, the rest in the order they came as superseded' {

        # ARRANGE
        $candidates = @(
            (New-Candidate -Id 'beta' -Folder 'only')
            (New-Candidate -Id 'alpha' -Folder 'first')
            (New-Candidate -Id 'alpha' -Folder 'second')
            (New-Candidate -Id 'alpha' -Folder 'third')
        )
        $expectedIds = @('alpha', 'beta')
        $expectedWinner = 'first'
        $expectedSuperseded = @('second', 'third')
        $expectedLoneSupersededCount = 0

        # ACT
        $actualGroups = @(Group-ModCandidate -Candidates $candidates)

        # ASSERT
        $actualGroups.Id | Should -Be $expectedIds
        $actualGroups[0].Winner.Folder.Name | Should -Be $expectedWinner
        $actualGroups[0].Superseded.Folder.Name | Should -Be $expectedSuperseded
        $actualGroups[1].Superseded.Count | Should -Be $expectedLoneSupersededCount
    }

    # The game skips a copy whose mod_info.json it cannot read, so the copy after it still wins.
    It 'keeps an unparsed copy in a group of its own, apart from a parsed copy under the same name' {

        # ARRANGE
        $candidates = @(
            (New-Candidate -Id 'alpha' -Folder 'alpha' -Status 'U')
            (New-Candidate -Id 'alpha' -Folder 'Alpha copy')
        )
        $expectedIds = @('alpha', 'alpha')
        $expectedWinners = @('alpha', 'Alpha copy')
        $expectedSupersededCounts = @(0, 0)

        # ACT
        $actualGroups = @(Group-ModCandidate -Candidates $candidates)

        # ASSERT
        $actualGroups.Id | Should -Be $expectedIds
        $actualGroups.Winner.Folder.Name | Should -Be $expectedWinners
        $actualGroups.ForEach({ $_.Superseded.Count }) | Should -Be $expectedSupersededCounts
    }

    # Two copies whose IDs differ only in case are two mods to the game, each loading its own winner.
    It 'keeps copies the mod ID comparer tells apart in separate groups' {

        # ARRANGE
        $candidates = @(
            (New-Candidate -Id 'Alpha' -Folder 'upper')
            (New-Candidate -Id 'alpha' -Folder 'lower')
        )
        $expectedIds = @('alpha', 'Alpha')
        $expectedWinners = @('lower', 'upper')
        $expectedSupersededCounts = @(0, 0)

        # ACT
        $actualGroups = @(Group-ModCandidate -Candidates $candidates)

        # ASSERT
        $actualGroups.Id | Should -BeExactly $expectedIds
        $actualGroups.Winner.Folder.Name | Should -Be $expectedWinners
        $actualGroups.ForEach({ $_.Superseded.Count }) | Should -Be $expectedSupersededCounts
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
