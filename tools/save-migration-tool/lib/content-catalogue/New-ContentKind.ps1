# Describes one kind of content: the parts that define its IDs, as New-ContentPart describes them,
# and the rules by which the game builds more IDs of it at startup from another kind's.
function New-ContentKind {

    param(
        [string] $Kind,

        [object[]] $Parts,

        # Each rule's FromKind, Suffix and IsSuffixedBaseSkipped: the base kind's every file-defined ID
        # plus the suffix, unless the rule skips a base already ending in it.
        [object[]] $Generated = @()
    )

    return [pscustomobject]@{
        Kind      = $Kind
        Generated = $Generated
        Parts     = $Parts
    }
}
