package kmlib.console.parsing;

/**
 * Built-in {@link ValueParser}s for the value kinds KMLib console commands
 * accept, so a command's parameter spec reads as data and no command
 * re-implements number parsing. Each numeric factory takes the "expected" clause
 * to report on malformed input, keeping the wording with the command that owns
 * the parameter.
 */
public final class ParameterValues {
    private ParameterValues() {
    }

    // Identity parser: any token is already a valid free-form id, so this never
    // fails - the command resolves the id later against live state.
    public static ValueParser<String> text() {
        return raw -> raw;
    }

    // A decimal number, rejecting non-numeric input with the given clause.
    public static ValueParser<Float> decimal(String expected) {
        return raw -> {
            try {
                return Float.valueOf(raw);
            } catch (NumberFormatException malformed) {
                throw new ValueParseException(expected);
            }
        };
    }

    // A decimal number constrained to be non-negative; non-numeric and negative
    // input report the same clause, since either is an invalid value.
    public static ValueParser<Float> nonNegativeDecimal(String expected) {
        return raw -> {
            float value;
            try {
                value = Float.parseFloat(raw);
            } catch (NumberFormatException malformed) {
                throw new ValueParseException(expected);
            }
            if (value < 0f) {
                throw new ValueParseException(expected);
            }
            return value;
        };
    }
}
