# Writes an unquoted value as the JSON of the value org.json reads it as, so ConvertFrom-Json reads the same value.
# A port of JSONObject.stringToValue in the json.jar the game ships:
# - true, false and null match in any case;
# - otherwise only text opening with a digit, a point or a sign can be a number;
# - 0x and hexadecimal digits are an int, when they fit one;
# - text holding . e or E is a double, and any other text a long, when Java parses it;
# - anything else is text.
# A double stays a double: 1.0f is written 1.0, not 1, so it is read back as a double and not a whole number.
function ConvertTo-JsonValueText {

    param([string] $Text)

    $literals = 'true', 'false', 'null'
    $numberStartPattern = '^[0-9.+-]'
    $hexadecimalIntegerPattern = '^0[xX](?<sign>[+-]?)(?<digits>[0-9a-fA-F]+)$'
    $doubleMarkers = [char[]] '.eE'

    $culture = [cultureinfo]::InvariantCulture
    $quotedText = "`"$Text`""

    $literal = $Text.ToLowerInvariant()

    if ($literal -cin $literals) {
        return $literal
    }

    if ($Text -cnotmatch $numberStartPattern) {
        return $quotedText
    }

    $hexadecimal = [regex]::Match(
        $Text,
        $hexadecimalIntegerPattern)

    if ($hexadecimal.Success) {

        # The leading 0 keeps the value positive: AllowHexSpecifier reads a high first digit as a sign bit.
        $magnitude = [System.Numerics.BigInteger]::Parse(
            "0$($hexadecimal.Groups['digits'].Value)",
            [Globalization.NumberStyles]::AllowHexSpecifier,
            $culture)

        $integer = if ($hexadecimal.Groups['sign'].Value -ceq '-') {
            -$magnitude
        } else {
            $magnitude
        }

        if ($integer -ge [int]::MinValue `
                -and $integer -le [int]::MaxValue) {

            return $integer.ToString($culture)
        }
    }

    if ($Text.IndexOfAny($doubleMarkers) -ge 0) {

        $double = ConvertFrom-JavaDoubleText -Text $Text

        if ($null -eq $double) {
            return $quotedText
        }

        # The game refuses it as an object's value (JSONObject.testValidity), and JSON cannot write it.
        if ([double]::IsInfinity($double)) {
            throw "$Text is too large for a double, which the game rejects"
        }

        $doubleText = $double.ToString('R', $culture)

        if ($doubleText.IndexOfAny($doubleMarkers) -lt 0) {
            $doubleText += '.0'
        }

        return $doubleText
    }

    $long = 0L

    if ([long]::TryParse(
            $Text,
            [Globalization.NumberStyles]::AllowLeadingSign,
            $culture,
            [ref] $long)) {

        return $long.ToString($culture)
    }
    return $quotedText
}
