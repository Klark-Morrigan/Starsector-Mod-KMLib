# Groups roster candidates by mod ID, and picks the copy the game loads in each: the highest version.
# Superseded holds the other copies, highest first.
# Copies whose version numbers tie are ordered by the version as written, highest first, so 1.5b beats 1.5a.
# Copies at the very same version are ordered by folder name, so the same install always picks the same winner.
function Group-ModCandidate {

    param([object[]] $Candidates)

    $highestNumbersFirst = @{ Expression = 'SortKey'; Descending = $true }
    $highestTextFirst = @{ Expression = { Format-ModVersion -Version $_.Version }; Descending = $true }
    $folderNameOrder = @{ Expression = { $_.Folder.Name }; Descending = $false }
    $copyOrder = @($highestNumbersFirst, $highestTextFirst, $folderNameOrder)

    foreach ($group in ($Candidates | Group-Object -Property Id)) {

        $ordered = @($group.Group | Sort-Object -Property $copyOrder -Stable)

        [pscustomobject]@{
            Id         = $group.Name
            Winner     = $ordered[0]
            Superseded = @($ordered | Select-Object -Skip 1)
        }
    }
}
