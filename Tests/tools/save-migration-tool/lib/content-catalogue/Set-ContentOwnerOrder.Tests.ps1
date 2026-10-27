BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Set-ContentOwnerOrder.ps1"
}

Describe 'Set-ContentOwnerOrder' {

    It 'puts every ID''s owners in the order of the sources' {

        $sources = @('beta', 'alpha', 'core') | ForEach-Object { [pscustomobject]@{ Owner = $_ } }
        $index = New-OwnerIndex @{ 'a' = @('core', 'alpha', 'beta'); 'b' = @('alpha', 'beta'); 'c' = @('core') }

        Set-ContentOwnerOrder -OwnersById $index -Sources $sources

        Format-OwnerIndex -OwnersById $index | Should -Be 'a=beta+alpha+core b=beta+alpha c=core'
    }
}
