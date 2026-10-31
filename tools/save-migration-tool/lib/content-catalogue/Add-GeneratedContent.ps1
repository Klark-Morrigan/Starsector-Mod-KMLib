# Adds the IDs the game builds at startup by one Generated rule of Get-ContentDefinitions:
# each base ID plus the rule's suffix, owned by whoever owns the base.
# A rule that skips suffixed bases adds nothing for a base whose ID already ends in the suffix,
# as the game makes no d-hull of a hull whose ID already names one.
# Returns how many IDs the rule built.
function Add-GeneratedContent {

    param(
        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $OwnersById,

        # The base kind's IDs as its files define them, taken before any rule adds to that kind.
        [string[]] $BaseIds,

        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]] $BaseOwners,

        [object] $Rule
    )

    $generatedCount = 0

    foreach ($baseId in $BaseIds) {

        $isSkipped = $Rule.IsSuffixedBaseSkipped -and $baseId.EndsWith($Rule.Suffix, [System.StringComparison]::Ordinal)

        if ($isSkipped) {
            continue
        }

        foreach ($owner in $BaseOwners[$baseId]) {
            Add-ContentOwner -OwnersById $OwnersById -Id "$baseId$($Rule.Suffix)" -Owner $owner
        }

        $generatedCount++
    }

    return $generatedCount
}
