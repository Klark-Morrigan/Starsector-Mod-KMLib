BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\Group-ModCandidate.ps1"
}

Describe 'Group-ModCandidate' {

    It 'picks the highest sort key per ID as the winner, the rest highest first as superseded' {

        $groups = @(Group-ModCandidate -Candidates @(
            [pscustomobject]@{ Id = 'alpha'; SortKey = '000001'; Name = 'old' }
            [pscustomobject]@{ Id = 'alpha'; SortKey = '000003'; Name = 'new' }
            [pscustomobject]@{ Id = 'alpha'; SortKey = '000002'; Name = 'middle' }
            [pscustomobject]@{ Id = 'beta'; SortKey = '000001'; Name = 'only' }
        ))

        $groups.Id | Should -Be @('alpha', 'beta')
        $groups[0].Winner.Name | Should -Be 'new'
        $groups[0].Superseded.Name | Should -Be @('middle', 'old')
        $groups[1].Superseded.Count | Should -Be 0
    }

    It 'returns nothing for no candidates' {
        @(Group-ModCandidate -Candidates @()).Count | Should -Be 0
    }
}
