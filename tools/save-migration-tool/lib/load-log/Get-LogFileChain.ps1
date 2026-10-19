# Returns a log and its rotated siblings, oldest first, so they read as one log.
# log4j rotation shifts starsector.log to .1 and each .N to .N+1, so the highest number is the oldest.
function Get-LogFileChain {

    param([string] $Path)

    $directory = Split-Path -Parent $Path
    $leafName = Split-Path -Leaf $Path
    $rotatedPattern = "^$([regex]::Escape($leafName))\.(?<index>\d+)$"

    $rotated = Get-ChildItem -LiteralPath $directory -File |
        Where-Object { $_.Name -match $rotatedPattern } |
        Sort-Object -Property { [int] ([regex]::Match($_.Name, $rotatedPattern).Groups['index'].Value) } -Descending

    # Comma-operator wrap so a log with no rotated siblings still returns an array, not a lone string.
    return ,@(@($rotated | ForEach-Object { $_.FullName }) + $Path)
}
