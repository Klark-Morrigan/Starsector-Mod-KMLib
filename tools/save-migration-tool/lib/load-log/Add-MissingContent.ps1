# Adds to a load, and to each of its attempts, the missing content its cause names.
# The load's own is its first attempt's, since that attempt read campaign.xml, the newest file.
#   Depends on Get-MissingContent being dot-sourced into the same scope.
function Add-MissingContent {

    param([object] $Load)

    foreach ($attempt in $Load.Attempts) {

        $missing = Get-MissingContent -CauseType $attempt.CauseType -Cause $attempt.Cause
        $attempt | Add-Member -NotePropertyName 'Missing' -NotePropertyValue $missing
    }

    $Load | Add-Member -NotePropertyName 'Missing' -NotePropertyValue $Load.Attempts[0].Missing

    return $Load
}
