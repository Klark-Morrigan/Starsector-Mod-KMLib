# Writes each attempt of a load in order: its source, then nested under it how it ended, where in the log it
# starts, and its cause. Writes nothing for a load tried once, whose one attempt adds nothing to the load's cause.
function Format-AttemptLines {

    param([object[]] $Attempts)

    if (@($Attempts).Count -lt 2) {
        return
    }

    'Attempts, in order:'

    foreach ($attempt in $Attempts) {

        $logFileName = Format-ConsoleText -Role 'Path' -Text (Split-Path -Leaf $attempt.LogFile)

        "  $(Format-ConsoleText -Role 'Path' -Text $attempt.Source):"
        "    $(Format-LoadOutcome -Outcome $attempt.Outcome) at ${logFileName}:$(Format-ConsoleNumber $attempt.LogLine)"

        if ($attempt.CauseType) {

            foreach ($causeLine in Format-CauseLines -CauseType $attempt.CauseType -Cause $attempt.Cause) {
                "    $causeLine"
            }
        }
    }
}
