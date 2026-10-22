BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Format-ModVersion.ps1"
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
    }

    It 'picks the highest sort key per ID as the winner, the rest highest first as superseded' {

        $groups = @(Group-ModCandidate -Candidates @(
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'old')
            (New-Candidate -Id 'alpha' -SortKey '000003' -Folder 'new')
            (New-Candidate -Id 'alpha' -SortKey '000002' -Folder 'middle')
            (New-Candidate -Id 'beta' -SortKey '000001' -Folder 'only')
        ))

        $groups.Id | Should -Be @('alpha', 'beta')
        $groups[0].Winner.Folder.Name | Should -Be 'new'
        $groups[0].Superseded.Folder.Name | Should -Be @('middle', 'old')
        $groups[1].Superseded.Count | Should -Be 0
    }

    It 'orders copies whose version numbers tie by the version as written, highest first' {

        $groups = @(Group-ModCandidate -Candidates @(
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'Alpha copy' -Version '1.5a')
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'Zeta copy' -Version '1.5b')
        ))

        $groups[0].Winner.Folder.Name | Should -Be 'Zeta copy'
    }

    It 'orders copies at the very same version by folder name, whatever order they came in' {

        $groups = @(Group-ModCandidate -Candidates @(
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'Zeta copy')
            (New-Candidate -Id 'alpha' -SortKey '000001' -Folder 'Alpha copy')
        ))

        $groups[0].Winner.Folder.Name | Should -Be 'Alpha copy'
        $groups[0].Superseded.Folder.Name | Should -Be @('Zeta copy')
    }

    It 'returns nothing for no candidates' {
        @(Group-ModCandidate -Candidates @()).Count | Should -Be 0
    }
}
