# The outcomes a save load attempt ends in.
# Reading the log sets them and grouping the attempts reads them, so they are named in one place.
function Get-LoadOutcomeNames {

    return [pscustomobject]@{
        Loaded     = 'Loaded'
        Failed     = 'Failed'

        # The log ends before the attempt does: the game crashed or is still loading.
        Unfinished = 'Unfinished'
    }
}
