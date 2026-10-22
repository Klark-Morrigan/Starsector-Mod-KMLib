# Writes Get-SaveLoadAttempt's loads as the text a person reads, one string per line, newest load first.
# Each load opens with a blank line, then its save and how it ended.
# The first load opens so too, which parts it from the progress lines written above it.
function Format-LoadListText {

    param([object[]] $Loads)

    if (@($Loads).Count -eq 0) {
        return @('', 'The logs hold no load to list.')
    }

    $lines = [System.Collections.Generic.List[string]]::new()

    foreach ($load in $Loads) {

        $save = Format-ConsoleText -Role 'Path' -Text $load.SaveFolder

        $lines.Add('')
        $lines.Add("${save}: $(Format-LoadOutcome -Outcome $load.Outcome)")
        $lines.AddRange([string[]] @(Format-LoadDetailLines -Load $load))
    }

    return $lines.ToArray()
}
