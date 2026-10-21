# Turns Invoke-SaveMigrationTool's report into the text a person reads, writing one string per line.
# The load's readings reuse the lines Get-SaveLoadAttempt writes to stderr, so both say the same thing.
function Format-DiagnosisReport {

    param([object] $Report)

    if (-not $Report.SaveFolder) {
        return 'No load failed in the logs, so no save is diagnosed. Name one with -SaveFolder.'
    }

    $lines = [System.Collections.Generic.List[string]]::new()
    $load = $Report.Load

    $lines.Add("Save: $($Report.SaveFolder)")

    if ($null -eq $load) {
        $lines.Add('Newest load: none in the logs')

    } else {

        $lines.Add("Newest load: $($load.Outcome)")

        if ($load.CauseType) {

            $lines.Add("  Cause: $($load.CauseType): $($load.Cause)")
            $lines.Add("  $(Format-MissingContentLine -Load $load)")
        }

        if (@($load.ObjectChain).Count -gt 0) {
            $lines.Add("  $(Format-ObjectChainLine -Load $load)")
        }
    }

    # The list alone says nothing a person acts on, so the text gives its size; JSON keeps the list for a caller.
    $lines.Add('')
    $lines.Add("Mods the save was last written with: $(@($Report.Mods).Count); Get-SaveModDrift.ps1 lists them")

    return $lines.ToArray()
}
