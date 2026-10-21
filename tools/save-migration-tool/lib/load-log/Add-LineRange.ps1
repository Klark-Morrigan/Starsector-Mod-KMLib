# Appends the lines from one index to another, both included, to a list. Adds nothing when the range is empty.
function Add-LineRange {

    param(
        [System.Collections.Generic.List[string]] $Target,
        [string[]] $Lines,
        [int] $From,
        [int] $To
    )

    if ($To -ge $From) {
        $Target.AddRange([System.ArraySegment[string]]::new($Lines, $From, $To - $From + 1))
    }
}
