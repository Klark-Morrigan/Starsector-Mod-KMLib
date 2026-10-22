# Builds an ordinally comparable key from either version shape:
# the "1.2.3" string form, a bare number, or the { major, minor, patch } object form.
# Components keep their leading digits only, so "91.DEV" ranks as 91.0 and "1.3.1b" as 1.3.1 rather than collapsing.
function Get-VersionSortKey {

    param([object] $Version)

    # Version components are zero-padded to this width so an ordinal string comparison matches numeric ordering
    # ("10" must sort above "9"). Wide enough for a date-stamped part such as 20240615123045.
    $versionPadWidth = 20

    # Components compared per version. Starsector versions are major.minor.patch
    # with an occasional build suffix, so four covers every observed shape.
    $versionPartCount = 4

    $versionSeparatorPattern = '[.\-_ ]'
    $leadingDigitsPattern = '^\d+'

    if ($null -eq $Version) {
        $parts = @()

    } elseif (Test-VersionObjectForm -Version $Version) {
        $parts = Get-VersionObjectParts -Version $Version

    } else {
        $parts = "$Version" -split $versionSeparatorPattern
    }

    $key = foreach ($index in 0..($versionPartCount - 1)) {

        $component = if ($index -lt $parts.Count) { "$($parts[$index])" } else { '' }
        $leadingDigits = [regex]::Match($component.Trim(), $leadingDigitsPattern)

        # Digits compare as text, so no part is too large for a number type.
        $digits = $leadingDigits.Value.TrimStart('0')

        if (-not $digits) {
            $digits = '0'
        }

        $digits.PadLeft($versionPadWidth, '0')
    }

    return ($key -join '.')
}
