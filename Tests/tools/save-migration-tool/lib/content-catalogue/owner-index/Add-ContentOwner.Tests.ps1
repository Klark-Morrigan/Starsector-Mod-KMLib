BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\content-catalogue\owner-index\Add-ContentOwner.ps1"
}

Describe 'Add-ContentOwner' {

    It 'adds an ID with its owner, and another owner to an ID it holds, each owner once' {

        # ARRANGE
        $actualIndex = New-OwnerIndex

        $expectedIndex = 'a=alpha+beta b=alpha'

        # ACT
        Add-ContentOwner -OwnersById $actualIndex -Id 'a' -Owner 'alpha'
        Add-ContentOwner -OwnersById $actualIndex -Id 'a' -Owner 'beta'
        Add-ContentOwner -OwnersById $actualIndex -Id 'a' -Owner 'alpha'
        Add-ContentOwner -OwnersById $actualIndex -Id 'b' -Owner 'alpha'

        # ASSERT
        Format-OwnerIndex -OwnersById $actualIndex | Should -Be $expectedIndex
    }

    # The game's spec stores look IDs up case-sensitively.
    It 'keeps IDs that differ only in case apart' {

        # ARRANGE
        $actualIndex = New-OwnerIndex

        $expectedIndex = 'A=beta a=alpha'

        # ACT
        Add-ContentOwner -OwnersById $actualIndex -Id 'a' -Owner 'alpha'
        Add-ContentOwner -OwnersById $actualIndex -Id 'A' -Owner 'beta'

        # ASSERT
        Format-OwnerIndex -OwnersById $actualIndex | Should -Be $expectedIndex
    }
}
