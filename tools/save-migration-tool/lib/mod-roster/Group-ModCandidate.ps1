# Groups roster candidates by mod ID, and picks the copy the game loads in each: the first in folder order.
# The game keeps the first copy of an ID it reads and drops the rest, whatever their versions.
# Superseded holds the other copies, in folder order.
# The game skips a copy whose mod_info.json it cannot read, so an unparsed copy never claims an ID:
# it stands alone, under its folder name.
# Groups come out in ID order for a person to read; only Get-ModIdComparer decides which copies share one.
function Group-ModCandidate {

    param([object[]] $Candidates)

    $parsedStatus = (Get-ModStatusNames).Parsed
    $copyGroups = [System.Collections.Generic.List[System.Collections.Generic.List[object]]]::new()
    $parsedCopiesById = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[object]]]::new(
        (Get-ModIdComparer))

    foreach ($candidate in $Candidates) {

        $isParsed = $candidate.Status -eq $parsedStatus

        if ($isParsed -and $parsedCopiesById.ContainsKey($candidate.Id)) {

            $parsedCopiesById[$candidate.Id].Add($candidate)
            continue
        }

        $copies = [System.Collections.Generic.List[object]]::new()

        $copies.Add($candidate)
        $copyGroups.Add($copies)

        if ($isParsed) {
            $parsedCopiesById[$candidate.Id] = $copies
        }
    }

    $groups = foreach ($copies in $copyGroups) {

        [pscustomobject]@{
            Id         = $copies[0].Id
            Winner     = $copies[0]
            Superseded = @($copies | Select-Object -Skip 1)
        }
    }

    return $groups | Sort-Object -Property Id -CaseSensitive -Stable
}
