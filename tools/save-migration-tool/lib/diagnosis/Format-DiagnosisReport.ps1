# Turns Invoke-SaveMigrationTool's report into the text a person reads, writing one string per line.
# The load reads as Get-SaveLoadAttempt -AsText writes it, so both say the same thing.
# The report opens with a blank line, which parts it from the progress lines written above it.
function Format-DiagnosisReport {

    param([object] $Report)

    if (-not $Report.SaveFolder) {

        $parameter = Format-ConsoleText -Role 'Code' -Text '-SaveFolder'

        return @('', "No load failed in the logs, so no save is diagnosed. Name one with $parameter.")
    }

    $lines = [System.Collections.Generic.List[string]]::new()
    $load = $Report.Load

    $lines.Add('')
    $lines.Add("Save: $(Format-ConsoleText -Role 'Path' -Text $Report.SaveFolder)")

    if ($null -eq $load) {
        $lines.Add('Newest load: none in the logs')

    } else {

        $lines.Add("Newest load: $(Format-LoadOutcome -Outcome $load.Outcome)")
        $lines.AddRange([string[]] @(Format-LoadDetailLines -Load $load))
    }

    # The list alone says nothing a person acts on, so the text gives its size; JSON keeps the list for a caller.
    $lines.Add('')
    $lines.Add(
        "Mods the save was last written with: $(Format-ConsoleNumber (@($Report.Mods).Count)); " +
        "$(Format-ConsoleText -Role 'Script' -Text 'Get-SaveModDrift.ps1') lists them")

    return $lines.ToArray()
}
