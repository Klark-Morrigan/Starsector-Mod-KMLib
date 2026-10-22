# Groups roster candidates by mod ID, and picks the copy the game loads in each: the highest version.
# Superseded holds the other copies, highest first.
function Group-ModCandidate {

    param([object[]] $Candidates)

    foreach ($group in ($Candidates | Group-Object -Property Id)) {

        $ordered = @($group.Group | Sort-Object -Property SortKey -Descending)

        [pscustomobject]@{
            Id         = $group.Name
            Winner     = $ordered[0]
            Superseded = @($ordered | Select-Object -Skip 1)
        }
    }
}
