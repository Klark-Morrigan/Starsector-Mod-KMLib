BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Get-BuiltInVariantIds.ps1"
}

Describe 'Get-BuiltInVariantIds' {

    # 0.98a builds 52 variants in code, lasher1 twice, so 51 IDs.
    It 'lists the 51 variants the game builds in its own code, each once' {

        $ids = @(Get-BuiltInVariantIds)

        $ids.Count | Should -Be 51
        @($ids | Select-Object -Unique).Count | Should -Be 51
        $ids[0] | Should -Be 'omen1'
        $ids[-1] | Should -Be 'atlas1'
    }
}
