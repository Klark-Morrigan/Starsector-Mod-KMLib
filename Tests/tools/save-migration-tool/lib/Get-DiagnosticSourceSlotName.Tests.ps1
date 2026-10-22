BeforeAll {
    . "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Get-DiagnosticSourceSlotName.ps1"
}

Describe 'Get-DiagnosticSourceSlotName' {

    It 'names a slot under the tool''s own prefix' {
        Get-DiagnosticSourceSlotName | Should -Be 'KMLib.SaveMigrationTool.LastDiagnosticSource'
    }
}
