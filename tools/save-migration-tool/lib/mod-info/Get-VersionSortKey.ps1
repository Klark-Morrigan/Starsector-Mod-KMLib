# Builds an ordinally comparable key from either version shape:
# the "1.2.3" string form or the { major, minor, patch } object form.
# Components keep their leading digits only, so "91.DEV" ranks as 91 and "1.3.1b" as 1 rather than collapsing to zero.
function Get-VersionSortKey {

    param([object] $Version)

    # Version components are zero-padded to this width so an ordinal string
    # comparison matches numeric ordering ("10" must sort above "9").
    $versionPadWidth = 6

    # Components compared per version. Starsector versions are major.minor.patch
    # with an occasional build suffix, so four covers every observed shape.
    $versionPartCount = 4

    if ($null -eq $Version) {
        $parts = @()

    } elseif ($Version -is [string] -or $Version -is [int] -or $Version -is [double]) {
        $parts = "$Version" -split '[.\-_ ]'

    } else {
        $parts = Get-VersionObjectParts -Version $Version
    }

    $key = foreach ($index in 0..($versionPartCount - 1)) {

        $component = if ($index -lt $parts.Count) { "$($parts[$index])" } else { '0' }
        $leadingDigits = [regex]::Match($component.Trim(), '^\d+')
        $number = if ($leadingDigits.Success) { [int] $leadingDigits.Value } else { 0 }
        $number.ToString().PadLeft($versionPadWidth, '0')
    }

    return ($key -join '.')
}
