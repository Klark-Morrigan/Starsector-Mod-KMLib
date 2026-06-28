package kmlib.console.parsing;

/**
 * Converts a raw command token into a typed parameter value, or fails with the
 * clause describing what a valid value looks like. An implementation carries
 * only the conversion; {@link ParameterParser} owns where the token comes from
 * (named or positional) and how a failure is framed for the player.
 */
@FunctionalInterface
public interface ValueParser<T> {
    T parseValue(String raw) throws ValueParseException;
}
