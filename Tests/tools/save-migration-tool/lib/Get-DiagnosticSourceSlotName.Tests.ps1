BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Get-DiagnosticSourceSlotName.ps1"
}

Describe 'Get-DiagnosticSourceSlotName' {

    # The slot is process-wide, so its name must be one no other tool in the process would pick.
    It 'names the slot under the tool''s own prefix' {

        # ARRANGE
        $expectedSlotName = 'KMLib.SaveMigrationTool.LastDiagnosticSource'

        # ACT
        $actualSlotName = Get-DiagnosticSourceSlotName

        # ASSERT
        $actualSlotName | Should -Be $expectedSlotName
    }
}
