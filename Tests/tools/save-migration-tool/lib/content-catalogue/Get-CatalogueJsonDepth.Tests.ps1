BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Get-CatalogueJsonDepth.ps1"
}

Describe 'Get-CatalogueJsonDepth' {

    # A shallower depth would write each kind's IDs as one space-joined string.
    It 'reaches each owner entry''s IDs by kind, as lists' {

        # ARRANGE
        $entries = @(
            [pscustomobject]@{ Owner = 'alpha'; Ids = [ordered]@{ Hull = [string[]] @('alpha_hull', 'beta_hull') } }
        )

        $expectedJson = '[{"Owner":"alpha","Ids":{"Hull":["alpha_hull","beta_hull"]}}]'

        # ACT
        $actualJson = ConvertTo-Json -InputObject $entries -Depth (Get-CatalogueJsonDepth) -Compress

        # ASSERT
        $actualJson | Should -Be $expectedJson
    }
}
