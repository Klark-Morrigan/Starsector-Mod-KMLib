package kmlib.mods.console.commands.parsing;

/**
 * Raised by a {@link ValueParser} when a raw token cannot become its typed
 * value. Carries only the "expected" clause (e.g. "a number in degrees per
 * day"); {@link ParameterParser} frames the full message with the parameter
 * name and the offending token, so every parameter reports malformed input the
 * same way.
 */
public final class ValueParseException extends Exception {
    public ValueParseException(String expected) {
        super(expected);
    }
}
