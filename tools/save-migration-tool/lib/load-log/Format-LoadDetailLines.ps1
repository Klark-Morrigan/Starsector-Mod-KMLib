# Writes what a person reads about one load, indented under a line naming it, one string per line:
# the cause and what it was read as, each retry vanilla made, and the mod classes the failure sat in.
function Format-LoadDetailLines {

    param([object] $Load)

    $indent = '  '

    $lines = @(
        if ($Load.CauseType) {

            'Cause:'
            Format-CauseLines -CauseType $Load.CauseType -Cause $Load.Cause | ForEach-Object { "$indent$_" }
        }

        Format-LoadReadingLines -Load $Load
        Format-AttemptLines -Attempts $Load.Attempts
        Format-ModClassLines -ObjectChain $Load.ObjectChain
    )

    return $lines | ForEach-Object { "$indent$_" }
}
