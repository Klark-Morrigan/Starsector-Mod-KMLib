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
    $readPartCounts = @(2, 3, 4)
    $joinedPartCount = 4
    $versionPartCount = 3

    # Java's split drops the empty strings a trailing separator leaves; .NET's keeps them.
    $parts = [System.Collections.Generic.List[string]]::new([regex]::Split($Text, $partSeparatorPattern))

    while ($parts.Count -gt 0 -and $parts[$parts.Count - 1] -eq '') {
        $parts.RemoveAt($parts.Count - 1)
    }

    $isRead = $parts.Count -in $readPartCounts
    $isFirstTwoJoined = $isRead -and ($parts.Count -eq $joinedPartCount -or $parts[0] -ceq $leadingZeroPart)

    if (-not $isRead) {
        $parts.Clear()

    } elseif ($isFirstTwoJoined) {
        $parts[1] = "$($parts[0]).$($parts[1])"
        $parts.RemoveAt(0)
    }

    while ($parts.Count -lt $versionPartCount) {
        $parts.Add('')
    }

    return New-VersionInfo `
        -Major $parts[0] `
        -Minor $parts[1] `
        -Patch $parts[2] `
        -MinorIsRC $Text.Contains($releaseCandidateMarker)
}
