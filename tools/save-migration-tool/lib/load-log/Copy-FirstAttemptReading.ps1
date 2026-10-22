# Gives a load its first attempt's cause, missing content and object chain.
# The first attempt read campaign.xml, the newest file, so its failure is the one the player met;
# the retries read older files vanilla fell back to.
function Copy-FirstAttemptReading {

    param([object] $Load)

    $firstAttempt = $Load.Attempts[0]

    $Load.CauseType = $firstAttempt.CauseType
    $Load.Cause = $firstAttempt.Cause
    $Load.Missing = $firstAttempt.Missing
    $Load.ObjectChain = $firstAttempt.ObjectChain

    return $Load
}
