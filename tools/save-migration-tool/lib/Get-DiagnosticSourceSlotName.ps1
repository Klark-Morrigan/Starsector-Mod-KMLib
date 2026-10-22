# Names the process-wide slot that remembers which script wrote the last diagnostic line.
# Each script dot-sources the library into its own scope, and one script runs another,
# so only a slot the whole process shares sees every script's lines.
function Get-DiagnosticSourceSlotName {

    return 'KMLib.SaveMigrationTool.LastDiagnosticSource'
}
