BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Add-ContentOwner.ps1"
}

Describe 'Add-ContentOwner' {

    It 'adds an ID with its owner, and another owner to an ID it holds, each owner once' {

        $index = New-OwnerIndex

        Add-ContentOwner -OwnersById $index -Id 'a' -Owner 'alpha'
        Add-ContentOwner -OwnersById $index -Id 'a' -Owner 'beta'
        Add-ContentOwner -OwnersById $index -Id 'a' -Owner 'alpha'
        Add-ContentOwner -OwnersById $index -Id 'b' -Owner 'alpha'

        Format-OwnerIndex -OwnersById $index | Should -Be 'a=alpha+beta b=alpha'
    }

    # The game's spec stores look IDs up case-sensitively.
    It 'keeps IDs that differ only in case apart' {

        $index = New-OwnerIndex

        Add-ContentOwner -OwnersById $index -Id 'a' -Owner 'alpha'
        Add-ContentOwner -OwnersById $index -Id 'A' -Owner 'beta'

        Format-OwnerIndex -OwnersById $index | Should -Be 'A=beta a=alpha'
    }
}
