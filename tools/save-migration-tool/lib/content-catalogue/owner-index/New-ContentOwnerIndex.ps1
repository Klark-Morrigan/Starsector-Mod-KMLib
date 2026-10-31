# Returns an empty owner index: each content ID mapped to the sources that define it.
# The game matches content IDs case-sensitively, and so do the index's keys.
function New-ContentOwnerIndex {

    return [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[string]]]::new()
}
