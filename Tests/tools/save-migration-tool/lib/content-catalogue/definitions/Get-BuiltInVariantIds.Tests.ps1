BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content-catalogue\definitions\Get-BuiltInVariantIds.ps1"
}

Describe 'Get-BuiltInVariantIds' {

    # 0.98a builds 52 variants in code, lasher1 twice, so 51 IDs.
    It 'lists the 51 variants the game builds in its own code, each once' {

        # ARRANGE
        $expectedCount = 51
        $expectedFirstId = 'omen1'
        $expectedLastId = 'atlas1'

        # ACT
        $actualIds = @(Get-BuiltInVariantIds)

        # ASSERT
        $actualIds.Count | Should -Be $expectedCount
        @($actualIds | Select-Object -Unique).Count | Should -Be $expectedCount
        $actualIds[0] | Should -Be $expectedFirstId
        $actualIds[-1] | Should -Be $expectedLastId
    }
}
