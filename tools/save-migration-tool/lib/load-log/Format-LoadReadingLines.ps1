# Writes what a load's failure was read as, one string per line: the missing content its cause names,
# then its object chain when the log names one. Writes nothing for a load with neither.
# Get-SaveLoadAttempt writes these lines to stderr and the text report writes them under the cause,
# so both read them from here and say the same thing.
function Format-LoadReadingLines {

    param([object] $Load)

    if ($Load.CauseType) {
        Format-MissingContentLine -Load $Load
    }

    if (@($Load.ObjectChain).Count -gt 0) {
        Format-ObjectChainLines -Load $Load
    }
}
