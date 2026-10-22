# Writes what a person reads about one load, indented under a line naming it, one string per line:
# the cause and what it was read as, each retry vanilla made, and the mod classes the failure sat in.
# Each part nests its values one per line, so a long cause or class name never runs into the next value.
# The readings reuse the lines Get-SaveLoadAttempt writes to stderr, so both say the same thing.
function Format-LoadDetailLines {

    param([object] $Load)

    $lines = [System.Collections.Generic.List[string]]::new()

    if ($Load.CauseType) {

        $lines.Add('  Cause:')
        $lines.Add("    $(Format-ConsoleText -Role 'Code' -Text $Load.CauseType)")
        $lines.Add("    $(Format-ConsoleText -Role 'Error' -Text $Load.Cause)")
        $lines.Add("  $(Format-MissingContentLine -Load $Load)")
    }

    if (@($Load.ObjectChain).Count -gt 0) {

        foreach ($chainLine in Format-ObjectChainLines -Load $Load) {
            $lines.Add("  $chainLine")
        }
    }

    $attempts = @($Load.Attempts)

    # A load tried once has nothing to add beyond its own cause.
    if ($attempts.Count -gt 1) {

        $lines.Add('  Attempts, in order:')

        foreach ($attempt in $attempts) {

            $location = "$(Format-ConsoleText -Role 'Path' -Text (Split-Path -Leaf $attempt.LogFile))" +
                ":$(Format-ConsoleNumber $attempt.LogLine)"

            $lines.Add("    $(Format-ConsoleText -Role 'Path' -Text $attempt.Source):")
            $lines.Add("      $(Format-LoadOutcome -Outcome $attempt.Outcome) at $location")

            if ($attempt.CauseType) {

                $lines.Add("      $(Format-ConsoleText -Role 'Code' -Text $attempt.CauseType)")
                $lines.Add("      $(Format-ConsoleText -Role 'Error' -Text $attempt.Cause)")
            }
        }
    }

    $modLinks = Select-ModChainLink -ObjectChain @($Load.ObjectChain)

    if ($modLinks.Count -gt 0) {

        $lines.Add('  Mod classes in the object chain, innermost first:')

        foreach ($link in $modLinks) {

            $owners = @($link.Owners | ForEach-Object { Format-ConsoleText -Role 'Mod' -Text $_ }) -join ', '
            $lines.Add("    depth $(Format-ConsoleNumber $link.Depth): " +
                "$(Format-ConsoleText -Role 'Code' -Text $link.Class) ($owners)")
        }
    }

    return $lines.ToArray()
}
