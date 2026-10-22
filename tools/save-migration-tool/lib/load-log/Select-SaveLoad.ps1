# Keeps the loads a caller asked for, newest first as given: one save's, one outcome's, or only the newest.
# It runs before class owners are resolved, since reading every mod's JARs costs seconds,
# and a caller asking about one load should not pay for every load in the logs.
function Select-SaveLoad {

    param(
        [object[]] $Loads,
        [string] $SaveFolder,
        [string] $Outcome,
        [switch] $IsNewestOnly
    )

    $outcomeNames = @((Get-LoadOutcomeNames).PSObject.Properties.Value)

    if ($Outcome -and $Outcome -cnotin $outcomeNames) {
        throw "no load outcome $Outcome; the outcomes are $($outcomeNames -join ', ')"
    }

    $selected = @($Loads | Where-Object {
        (-not $SaveFolder -or $_.SaveFolder -eq $SaveFolder) -and (-not $Outcome -or $_.Outcome -ceq $Outcome)
    })

    if ($IsNewestOnly) {
        $selected = @($selected | Select-Object -First 1)
    }

    return ,$selected
}
