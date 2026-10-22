# Writes what a person reads about one load, indented under a line naming it, one string per line:
# the cause and what it was read as, each retry vanilla made, and the mod classes the failure sat in.
# The readings are the ones Get-SaveLoadAttempt writes to stderr, so both say the same thing.
function Format-LoadDetailLines {

    param([object] $Load)

    $indent = '  '

    $lines = @(
        if ($Load.CauseType) {

            'Cause:'
            Format-CauseLines -CauseType $Load.CauseType -Cause $Load.Cause | ForEach-Object { "$indent$_" }
            Format-MissingContentLine -Load $Load
        }

        if (@($Load.ObjectChain).Count -gt 0) {
            Format-ObjectChainLines -Load $Load
        }

        Format-AttemptLines -Attempts $Load.Attempts
        Format-ModClassLines -ObjectChain $Load.ObjectChain
    )

    return $lines | ForEach-Object { "$indent$_" }
}
