BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\content-catalogue\owner-index\Add-ContentOwner.ps1"
    . "$libRoot\content-catalogue\owner-index\Merge-ContentOwners.ps1"
}

Describe 'Merge-ContentOwners' {

    BeforeEach {

        $script:Added = [System.Collections.Generic.List[string]]::new()

        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }
    }

    It 'adds every owner of an ID both indexes hold to the first, and returns the first' {

        # ARRANGE
        $into = New-OwnerIndex @{ 'a' = @('alpha') }
        $from = New-OwnerIndex @{ 'a' = @('beta', 'core') }

        $expectedAdded = @('a=beta', 'a=core')
        $expectedAddCount = 2

        # ACT
        $actualMerged = Merge-ContentOwners -Into $into -From $from

        # ASSERT
        [object]::ReferenceEquals($actualMerged, $into) | Should -BeTrue
        $script:Added | Should -Be $expectedAdded

        Should -Invoke Add-ContentOwner -Times $expectedAddCount -Exactly -ParameterFilter {
            [object]::ReferenceEquals($OwnersById, $into)
        }
    }

    # The second index's lists stay its own, so a later owner added to the first never reaches them.
    It 'gives an ID only the second index holds a copy of its owners' {

        # ARRANGE
        $into = New-OwnerIndex @{ 'a' = @('alpha') }
        $from = New-OwnerIndex @{ 'b' = @('beta', 'core') }

        $expectedMerged = 'a=alpha b=beta+core'
        $expectedAddCount = 0

        # ACT
        $actualMerged = Merge-ContentOwners -Into $into -From $from

        # ASSERT
        Format-OwnerIndex -OwnersById $actualMerged | Should -Be $expectedMerged
        [object]::ReferenceEquals($actualMerged['b'], $from['b']) | Should -BeFalse

        Should -Invoke Add-ContentOwner -Times $expectedAddCount -Exactly
    }

    It 'returns the second index as it is when the first is empty' {

        # ARRANGE
        $into = New-OwnerIndex
        $from = New-OwnerIndex @{ 'b' = @('beta') }

        $expectedAddCount = 0

        # ACT
        $actualMerged = Merge-ContentOwners -Into $into -From $from

        # ASSERT
        [object]::ReferenceEquals($actualMerged, $from) | Should -BeTrue

        Should -Invoke Add-ContentOwner -Times $expectedAddCount -Exactly
    }
}
