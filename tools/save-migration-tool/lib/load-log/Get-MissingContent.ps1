# Reads which content a failed load's root cause names: its kind and ID.
# `Id` is what to look for in a mod; `ReportedId` is the ID as the message names it.
# The two differ for a d-hull, which names its base hull.
# Returns null when the cause names no missing content.
function Get-MissingContent {

    param(
        [string] $CauseType,
        [string] $Cause
    )

    $kinds = Get-ContentKindNames

    # The message each spec store throws when a save names an ID it lacks, one wording per store.
    # No two patterns match one message, so their order does not matter.
    $missingSpecPatterns = @(
        @{ Kind = $kinds.Weapon; Pattern = '^Weapon spec \[(?<id>[^\]]+)\] not found!$' }
        @{ Kind = $kinds.Hull; Pattern = '^Ship hull spec \[(?<id>[^\]]+)\] not found!$' }
        @{ Kind = $kinds.Variant; Pattern = '^Ship hull variant \[(?<id>[^\]]+)\] not found!$' }
        @{ Kind = $kinds.FighterWing; Pattern = '^Fighter wing spec \[(?<id>[^\]]+)\] not found$' }
        @{ Kind = $kinds.Hullmod; Pattern = '^Hullmod with id \[(?<id>[^\]]+)\] not found$' }

        # Every other spec type shares one generic store, whose message names the spec's class instead of a kind.
        @{ Kind = $kinds.Spec; Pattern = '^Spec of class \[(?<class>[^\]]+)\] with id \[(?<id>[^\]]+)\] not found$' }
    )

    # XStream throws this when a save names a class no loaded JAR holds; its message is the class name alone.
    $unresolvedClassExceptionType = 'com.thoughtworks.xstream.mapper.CannotResolveClassException'

    # Vanilla builds every hull's d-hull at startup, under the hull ID plus this suffix (Misc.D_HULL_SUFFIX).
    # A missing d-hull so means its base hull is missing, and the base hull is what a mod defines.
    $dHullSuffix = '_default_D'

    if ($CauseType -eq $unresolvedClassExceptionType) {
        return [pscustomobject]@{ Kind = $kinds.Class; Id = $Cause; ReportedId = $Cause; SpecClass = $null }
    }

    foreach ($missingSpec in $missingSpecPatterns) {

        $match = [regex]::Match($Cause, $missingSpec.Pattern)

        if (-not $match.Success) {
            continue
        }

        $reportedId = $match.Groups['id'].Value
        $isDHull = $missingSpec.Kind -eq $kinds.Hull -and
            $reportedId.EndsWith($dHullSuffix, [System.StringComparison]::Ordinal)

        $specClass = $match.Groups['class']

        $id = if ($isDHull) {
            $reportedId.Substring(0, $reportedId.Length - $dHullSuffix.Length)
        } else {
            $reportedId
        }

        $specClassName = if ($specClass.Success) {
            $specClass.Value
        } else {
            $null
        }

        return [pscustomobject]@{
            Kind       = $missingSpec.Kind
            Id         = $id
            ReportedId = $reportedId
            SpecClass  = $specClassName
        }
    }

    return $null
}
