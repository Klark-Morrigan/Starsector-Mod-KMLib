# Reads text as Java's Double.valueOf reads it, which org.json calls on an unquoted value holding . e or E.
# Java takes a float or double suffix (0.5f, 2d) and a hexadecimal significand with a binary exponent (0x1.8p1),
# which .NET's parse refuses, so the text is matched to Java's grammar first.
# Java's NaN and Infinity are left out: neither holds . e or E, so org.json never reads one as a double.
# Returns nothing for text Java refuses, and infinity for a number too large for a double, as Java does.
function ConvertFrom-JavaDoubleText {

    param([string] $Text)

    $hexadecimalDigitBits = 4
    $decimalPattern = '^[+-]?(?:[0-9]+\.?[0-9]*|\.[0-9]+)(?:[eE][+-]?[0-9]+)?[fFdD]?$'

    $hexadecimalPattern =
        '^(?<sign>[+-]?)0[xX](?<whole>[0-9a-fA-F]*)\.?(?<fraction>[0-9a-fA-F]*)[pP](?<exponent>[+-]?[0-9]+)[fFdD]?$'

    # Clamped far past the double's range, where the result is already zero or infinity, so it fits an int.
    $exponentLimit = 10000
    $culture = [cultureinfo]::InvariantCulture

    if ($Text -cmatch $decimalPattern) {

        return [double]::Parse(
            $Text.TrimEnd('f', 'F', 'd', 'D'),
            [Globalization.NumberStyles]::Float,
            $culture)
    }

    $hexadecimal = [regex]::Match(
        $Text,
        $hexadecimalPattern)

    $significandDigits = $hexadecimal.Groups['whole'].Value
        + $hexadecimal.Groups['fraction'].Value

    if (-not $hexadecimal.Success `
            -or $significandDigits.Length -eq 0) {

        return
    }

    # The leading 0 keeps the significand positive: AllowHexSpecifier reads a high first digit as a sign bit.
    $significand = [System.Numerics.BigInteger]::Parse(
        "0$significandDigits",
        [Globalization.NumberStyles]::AllowHexSpecifier,
        $culture)

    $exponent = [Math]::Clamp(
        [double] $hexadecimal.Groups['exponent'].Value,
        -$exponentLimit,
        $exponentLimit)

    $scale = [int] $exponent - $hexadecimalDigitBits * $hexadecimal.Groups['fraction'].Value.Length

    $magnitude = [Math]::ScaleB(
        [double] $significand,
        $scale)

    # PowerShell's unary minus turns 0.0 into 0 - 0.0, a positive zero, where Java keeps -0x0p0 negative.
    if ($hexadecimal.Groups['sign'].Value -ceq '-') {
        return [Math]::CopySign($magnitude, -1.0)
    }

    return $magnitude
}
