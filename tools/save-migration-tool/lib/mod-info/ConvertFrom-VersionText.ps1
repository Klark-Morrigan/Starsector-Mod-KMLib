# Reads a version written as text into its parts, the way the game's VersionInfo.setFromString does,
# so a version compares here as it compares in the game, quirks included.
# The game splits on dots and on the letter a, so "1.14.1aa" reads as 1, 14, 1.
# A first part of 0 joins the second as the major part, so "0.1.98.7" reads as major 0.1, minor 98, patch 7.
# Text of one part, or of five or more, sets no parts at all, and the game then cannot rank a change to it.
function ConvertFrom-VersionText {

    param([string] $Text)

    # The game's split pattern, alternatives in its order: a dot, the release-candidate marker, the letter a.
    $partSeparatorPattern = '\.|a-RC|a'
    $releaseCandidateMarker = '-RC'
    $leadingZeroPart = '0'

    # Java's split drops the empty strings a trailing separator leaves; .NET's keeps them.
    $parts = [System.Collections.Generic.List[string]]::new([regex]::Split($Text, $partSeparatorPattern))

    while ($parts.Count -gt 0 -and $parts[$parts.Count - 1] -eq '') {
        $parts.RemoveAt($parts.Count - 1)
    }

    $isMinorRC = $Text.Contains($releaseCandidateMarker)
    $isLeadingZero = $parts.Count -gt 0 -and $parts[0] -ceq $leadingZeroPart

    switch ($parts.Count) {

        2 {
            if ($isLeadingZero) {
                return New-VersionInfo -Major "$($parts[0]).$($parts[1])" -MinorIsRC $isMinorRC
            }

            return New-VersionInfo -Major $parts[0] -Minor $parts[1] -MinorIsRC $isMinorRC
        }

        3 {
            if ($isLeadingZero) {
                return New-VersionInfo -Major "$($parts[0]).$($parts[1])" -Minor $parts[2] -MinorIsRC $isMinorRC
            }

            return New-VersionInfo -Major $parts[0] -Minor $parts[1] -Patch $parts[2] -MinorIsRC $isMinorRC
        }

        4 {
            return New-VersionInfo `
                -Major "$($parts[0]).$($parts[1])" `
                -Minor $parts[2] `
                -Patch $parts[3] `
                -MinorIsRC $isMinorRC
        }
    }

    return New-VersionInfo -MinorIsRC $isMinorRC
}
