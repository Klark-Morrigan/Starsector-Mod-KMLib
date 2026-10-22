# Fails naming the outcomes there are when the given one is none of them.
# A caller checks before reading the logs, so a mistyped outcome costs no read.
function Assert-LoadOutcomeName {

    param([string] $Outcome)

    $outcomeNames = @((Get-LoadOutcomeNames).PSObject.Properties.Value)

    if ($Outcome -cnotin $outcomeNames) {
        throw "no load outcome $Outcome; the outcomes are $($outcomeNames -join ', ')"
    }
}
