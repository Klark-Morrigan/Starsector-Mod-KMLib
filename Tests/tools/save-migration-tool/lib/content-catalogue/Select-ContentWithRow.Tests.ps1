BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue"

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\Add-ContentOwner.ps1"
    . "$libRoot\New-ContentOwnerIndex.ps1"
    . "$libRoot\Select-ContentWithRow.ps1"
}

Describe 'Select-ContentWithRow' {

    BeforeEach {

        $script:Added = [System.Collections.Generic.List[string]]::new()

        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }
    }

    It 'keeps an ID both a file and a row define, with the file''s owners and the row''s' {

        # ARRANGE
        $fileOwners = New-OwnerIndex @{ 'both' = @('alpha'); 'file_only' = @('alpha') }
        $rowOwners = New-OwnerIndex @{ 'both' = @('beta', 'alpha'); 'row_only' = @('beta') }

        $expectedAdded = @('both=alpha', 'both=beta', 'both=alpha')

        # ACT
        Select-ContentWithRow -FileOwners $fileOwners -RowOwners $rowOwners | Out-Null

        # ASSERT
        $script:Added | Should -Be $expectedAdded
    }

    # Selecting is a read, so the given indexes must come out as they went in.
    It 'returns a new index, leaving both given ones as they were' {

        # ARRANGE
        $fileOwners = New-OwnerIndex @{ 'both' = @('alpha') }
        $rowOwners = New-OwnerIndex @{ 'both' = @('beta') }

        # ACT
        $actualSelected = Select-ContentWithRow -FileOwners $fileOwners -RowOwners $rowOwners

        # ASSERT
        [object]::ReferenceEquals($actualSelected, $fileOwners) | Should -BeFalse
        [object]::ReferenceEquals($actualSelected, $rowOwners) | Should -BeFalse
        Should -Invoke Add-ContentOwner -Times 2 -Exactly -ParameterFilter {
            [object]::ReferenceEquals($OwnersById, $actualSelected)
        }
    }
}
