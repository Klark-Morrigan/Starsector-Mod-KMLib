BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\content-catalogue\owner-index\Set-ContentOwnerOrder.ps1"
}

Describe 'Set-ContentOwnerOrder' {

    It 'puts every ID''s owners in the order of the sources' {

        # ARRANGE
        $sources = @('beta', 'alpha', 'core') | ForEach-Object { [pscustomobject]@{ Owner = $_ } }
        $actualIndex = New-OwnerIndex @{ 'a' = @('core', 'alpha', 'beta'); 'b' = @('alpha', 'beta'); 'c' = @('core') }

        $expectedIndex = 'a=beta+alpha+core b=beta+alpha c=core'

        # ACT
        Set-ContentOwnerOrder -OwnersById $actualIndex -Sources $sources

        # ASSERT
        Format-OwnerIndex -OwnersById $actualIndex | Should -Be $expectedIndex
    }
}
