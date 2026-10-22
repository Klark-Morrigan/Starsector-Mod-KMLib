# Paints how a load ended: a load that worked green, a failure red, a load the log ends inside amber.
function Format-LoadOutcome {

    param([string] $Outcome)

    $outcomes = Get-LoadOutcomeNames
    $outcomeRoles = @{
        $outcomes.Loaded     = 'Success'
        $outcomes.Failed     = 'Error'
        $outcomes.Unfinished = 'Warning'
    }

    return Format-ConsoleText -Role $outcomeRoles[$Outcome] -Text $Outcome
}
