# The outcomes a save load attempt ends in.
# Named once, so the code that sets an outcome and the code that reads it agree.
function Get-LoadOutcomeNames {

    return [pscustomobject]@{
        Loaded     = 'Loaded'
        Failed     = 'Failed'

        # The log ends before the attempt does: the game crashed or is still loading.
        Unfinished = 'Unfinished'
    }
}
