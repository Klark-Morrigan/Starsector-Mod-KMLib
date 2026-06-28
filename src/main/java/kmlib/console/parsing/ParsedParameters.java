package kmlib.console.parsing;

import org.lazywizard.console.BaseCommand.CommandResult;

import java.util.Map;

/**
 * The outcome of a {@link ParameterParser} run: whether every token was
 * accepted and, when it was, the typed value for each parameter. Mirrors the
 * command guard idiom -
 * {@code if (!parsed.isValid()) return parsed.getResult();} - then a command
 * reads values by their {@link Parameter} key. A value not supplied on the
 * command line falls back to the parameter's default (null when it has none);
 * {@link #isSupplied} distinguishes the two so a command can apply
 * cross-parameter rules that hinge on what the player actually typed.
 */
public final class ParsedParameters {
    private final boolean valid;
    private final CommandResult result;
    // Only the parameters the player supplied; an absent key falls back to its
    // parameter's default.
    private final Map<Parameter<?>, Object> suppliedValues;

    private ParsedParameters(boolean valid, CommandResult result,
            Map<Parameter<?>, Object> suppliedValues) {
        this.valid = valid;
        this.result = result;
        this.suppliedValues = suppliedValues;
    }

    static ParsedParameters valid(Map<Parameter<?>, Object> suppliedValues) {
        return new ParsedParameters(true, null, suppliedValues);
    }

    static ParsedParameters invalid(CommandResult result) {
        return new ParsedParameters(false, result, Map.of());
    }

    public boolean isValid() {
        return valid;
    }

    /**
     * @return the result to return from the command; meaningful only when
     *         {@link #isValid()} is false
     */
    public CommandResult getResult() {
        return result;
    }

    /**
     * The typed value for {@code parameter}: what the player supplied, or the
     * parameter's default when they did not.
     */
    public <T> T get(Parameter<T> parameter) {
        if (suppliedValues.containsKey(parameter)) {
            @SuppressWarnings("unchecked")
            T value = (T) suppliedValues.get(parameter);
            return value;
        }
        return parameter.getDefaultValue();
    }

    // Whether the player supplied this parameter explicitly, as opposed to it
    // falling back to a default, so a command can gate cross-parameter rules on
    // real input.
    public boolean isSupplied(Parameter<?> parameter) {
        return suppliedValues.containsKey(parameter);
    }
}
