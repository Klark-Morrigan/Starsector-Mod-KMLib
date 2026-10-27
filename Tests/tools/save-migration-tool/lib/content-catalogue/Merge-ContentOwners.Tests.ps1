BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Add-ContentOwner.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Merge-ContentOwners.ps1"
}

Describe 'Merge-ContentOwners' {

    BeforeEach {

        $script:Added = [System.Collections.Generic.List[string]]::new()

        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }
    }

    It 'adds every owner of every ID the second index holds to the first, and returns the first' {

        $into = New-OwnerIndex @{ 'a' = @('alpha') }
        $from = New-OwnerIndex @{ 'b' = @('beta', 'core') }

        $merged = Merge-ContentOwners -Into $into -From $from

        [object]::ReferenceEquals($merged, $into) | Should -BeTrue
        $script:Added | Should -Be @('b=beta', 'b=core')

        Should -Invoke Add-ContentOwner -Times 2 -Exactly -ParameterFilter {
            [object]::ReferenceEquals($OwnersById, $into)
        }
    }

    It 'returns the second index as it is when the first is empty' {

        $from = New-OwnerIndex @{ 'b' = @('beta') }

        $merged = Merge-ContentOwners -Into (New-OwnerIndex) -From $from

        [object]::ReferenceEquals($merged, $from) | Should -BeTrue

        Should -Invoke Add-ContentOwner -Times 0 -Exactly
    }
}
