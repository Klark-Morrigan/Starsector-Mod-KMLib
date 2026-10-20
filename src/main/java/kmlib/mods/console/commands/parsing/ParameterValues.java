package kmlib.mods.console.commands.parsing;

import kmlib.mods.console.ConsoleStrings;
import kmlib.starsector.strings.KmlibStringKeys;

import java.util.LinkedHashMap;
import java.util.Map;

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

    // Identity parser: any token is already a valid free-form ID, so this never
    // fails - the command resolves the ID later against live state.
    public static ValueParser<String> text() {
        return raw -> raw;
    }

    // A whole number greater than zero: what a "how many of them" parameter takes,
    // where zero and below would ask for nothing rather than for some.
    public static ValueParser<Integer> positiveWholeNumber(String expected) {

        return raw -> {

            int value;

            try {
                value = Integer.parseInt(raw);

            } catch (NumberFormatException numberFormatException) {
                throw new ValueParseException(expected);
            }

            if (value <= 0) {
                throw new ValueParseException(expected);
            }
            return value;
        };
    }

    // One of a fixed set of words, matched case-insensitively, standing for the value it selects.
    // The clause a stray word is refused with lists the words, so it never needs writing out. Worded
    // at the refusal rather than here, since a spec is often built before the game's strings load.
    public static <T> ValueParser<T> oneOfKeywords(Map<String, T> valuesByKeyword) {

        var keptValuesByKeyword = new LinkedHashMap<>(valuesByKeyword);

        return raw -> {

            for (var entry : keptValuesByKeyword.entrySet()) {

                if (entry.getKey().equalsIgnoreCase(raw)) {
                    return entry.getValue();
                }
            }

            throw new ValueParseException(ConsoleStrings.format(
                KmlibStringKeys.CATEGORY,
                KmlibStringKeys.CONSOLE_EXPECTED_ONE_OF,
                String.join(", ", keptValuesByKeyword.keySet())));
        };
    }

    // A decimal number, rejecting non-numeric input with the given clause.
    public static ValueParser<Float> decimal(String expected) {

        return raw -> {

            try {
                return Float.valueOf(raw);

            } catch (NumberFormatException numberFormatException) {
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

            } catch (NumberFormatException numberFormatException) {
                throw new ValueParseException(expected);
            }

            if (value < 0f) {
                throw new ValueParseException(expected);
            }
            return value;
        };
    }
}
