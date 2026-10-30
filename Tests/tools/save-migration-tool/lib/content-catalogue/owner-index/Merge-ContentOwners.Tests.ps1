BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\content-catalogue\owner-index\Add-ContentOwner.ps1"
    . "$libRoot\content-catalogue\owner-index\Merge-ContentOwners.ps1"
}

Describe 'Merge-ContentOwners' {

    BeforeEach {

        $script:Added = [System.Collections.Generic.List[string]]::new()

        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }
    }

    It 'adds every owner of every ID the second index holds to the first, and returns the first' {

        # ARRANGE
        $into = New-OwnerIndex @{ 'a' = @('alpha') }
        $from = New-OwnerIndex @{ 'b' = @('beta', 'core') }

        $expectedAdded = @('b=beta', 'b=core')
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
