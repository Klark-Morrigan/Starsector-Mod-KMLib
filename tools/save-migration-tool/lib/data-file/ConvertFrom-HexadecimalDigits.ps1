# Reads hexadecimal digits as the whole number they write, however many there are, never negative.
# The leading 0 keeps the number positive: AllowHexSpecifier reads a high first digit as a sign bit.
function ConvertFrom-HexadecimalDigits {

    param([string] $Digits)

    return [System.Numerics.BigInteger]::Parse(
        "0$Digits",
        [Globalization.NumberStyles]::AllowHexSpecifier,
        [cultureinfo]::InvariantCulture)
}
