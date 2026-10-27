# Groups roster candidates by mod ID, and picks the copy the game loads in each: the highest version.
# Superseded holds the other copies, highest first.
# Copies whose version numbers tie are ordered by the version as written, highest first, so 1.5b beats 1.5a.
# Copies at the very same version are ordered by folder name, so the same install always picks the same winner.
# Groups come out in ID order for a person to read; only Get-ModIdComparer decides which copies share one.
function Group-ModCandidate {

    param([object[]] $Candidates)

    $highestNumbersFirst = @{ Expression = 'SortKey'; Descending = $true }
    $highestTextFirst = @{ Expression = { Format-ModVersion -Version $_.Version }; Descending = $true }
    $folderNameOrder = @{ Expression = { $_.Folder.Name }; Descending = $false }
    $copyOrder = @($highestNumbersFirst, $highestTextFirst, $folderNameOrder)
    $copiesById = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[object]]]::new(
        (Get-ModIdComparer))

    foreach ($candidate in $Candidates) {

        if (-not $copiesById.ContainsKey($candidate.Id)) {
            $copiesById[$candidate.Id] = [System.Collections.Generic.List[object]]::new()
        }

        $copiesById[$candidate.Id].Add($candidate)
    }

    foreach ($id in ($copiesById.Keys | Sort-Object -CaseSensitive)) {

        $ordered = @($copiesById[$id] | Sort-Object -Property $copyOrder -Stable)

        [pscustomobject]@{
            Id         = $id
            Winner     = $ordered[0]
            Superseded = @($ordered | Select-Object -Skip 1)
        }
    }
}
