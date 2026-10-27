# Keeps the loads a caller asked for, newest first as given: one save's, one outcome's, or only the newest.
# It runs before any trace is read or class owner resolved, since those cost seconds,
# and a caller asking about one load should not pay for every load in the logs.
function Select-SaveLoad {

    param(
        [object[]] $Loads,
        [string] $SaveFolder,

        # A name Assert-LoadOutcomeName has passed, checked before any log is read.
        [string] $Outcome,

        [switch] $IsNewestOnly
    )

    $selected = @($Loads | Where-Object {
        (-not $SaveFolder -or $_.SaveFolder -eq $SaveFolder) -and (-not $Outcome -or $_.Outcome -ceq $Outcome)
    })

    if ($IsNewestOnly) {
        $selected = @($selected | Select-Object -First 1)
    }

    return ,$selected
}
