BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Add-ContentOwner.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Select-ContentWithRow.ps1"
}

Describe 'Select-ContentWithRow' {

    BeforeEach {

        $script:Added = [System.Collections.Generic.List[string]]::new()

        Mock Add-ContentOwner { $script:Added.Add("$Id=$Owner") }
    }

    It 'keeps an ID both a file and a row define, with the file''s owners and the row''s' {

        $fileOwners = New-OwnerIndex @{ 'both' = @('alpha'); 'file_only' = @('alpha') }
        $rowOwners = New-OwnerIndex @{ 'both' = @('beta', 'alpha'); 'row_only' = @('beta') }

        Select-ContentWithRow -FileOwners $fileOwners -RowOwners $rowOwners | Out-Null

        $script:Added | Should -Be @('both=alpha', 'both=beta', 'both=alpha')
    }

    It 'returns a new index, leaving both given ones as they were' {

        $fileOwners = New-OwnerIndex @{ 'both' = @('alpha') }
        $rowOwners = New-OwnerIndex @{ 'both' = @('beta') }

        $selected = Select-ContentWithRow -FileOwners $fileOwners -RowOwners $rowOwners

        [object]::ReferenceEquals($selected, $fileOwners) | Should -BeFalse
        [object]::ReferenceEquals($selected, $rowOwners) | Should -BeFalse

        Should -Invoke Add-ContentOwner -Times 2 -Exactly -ParameterFilter {
            [object]::ReferenceEquals($OwnersById, $selected)
        }
    }
}
