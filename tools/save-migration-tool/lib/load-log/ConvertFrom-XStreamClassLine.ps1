# Reads one line of XStream's debugging information as a class in the failed object's chain,
# or null for any other line.
# "class" names the object that failed, at depth 0. "class[n]" names its n-th container, counting outwards.
function ConvertFrom-XStreamClassLine {

    param([string] $Line)

    $classLinePattern = '^class(?:\[(?<depth>\d+)\])?\s+: (?<class>\S+)$'

    $match = [regex]::Match($Line, $classLinePattern)

    if (-not $match.Success) {
        return $null
    }

    $depth = $match.Groups['depth']

    return [pscustomobject]@{
        Depth = if ($depth.Success) { [int] $depth.Value } else { 0 }
        Class = $match.Groups['class'].Value
    }
}
