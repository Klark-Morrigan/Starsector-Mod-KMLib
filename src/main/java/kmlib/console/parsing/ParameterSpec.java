package kmlib.console.parsing;

import kmlib.console.output.CommandOutput;
import kmlib.console.output.ConsoleCommandOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * A console command's parameter specification: the ordered parameters it accepts
 * and the usage line shown when too many are given, declared as one cohesive
 * unit. A command subclasses this and declares each parameter as a field through
 * {@link #acceptsPositional} / {@link #acceptsNamed} / {@link #acceptsFlag}, so
 * the spec reads top to bottom as "what this command accepts" and those fields
 * double as the type-safe keys for reading values back from
 * {@link ParsedParameters}:
 *
 * <pre>
 * private static final class SpawnSpec extends ParameterSpec {
 *     final Parameter&lt;String&gt; focus = acceptsPositional("focus", "&lt;id&gt;", text());
 *     final Parameter&lt;Float&gt;  speed = acceptsPositional("speed", "&lt;deg/day&gt;", decimal(...));
 *     SpawnSpec() { super("Usage: ..."); }
 * }
 * </pre>
 *
 * <p>Each {@code acceptsX} call both records the parameter - in declaration
 * order, which is the positional order - and returns its key, so a command never
 * registers a parameter separately from declaring it. Parsing is delegated to
 * {@link ParameterParser}: a command calls {@link #parse} and reads values back
 * by the field keys.
 */
public abstract class ParameterSpec {
    private final List<Parameter<?>> parameters = new ArrayList<>();
    private final String usage;

    protected ParameterSpec(String usage) {
        this.usage = usage;
    }

    /**
     * A spec that accepts no parameters, carrying the given usage line - for a
     * command that takes no arguments but still wants a stray one reported as bad
     * syntax rather than silently ignored. Spares such a command an empty
     * subclass that would only call {@code super(usage)}.
     */
    public static ParameterSpec takingNoArguments(String usage) {
        return new NoArgumentsSpec(usage);
    }

    /**
     * Parses a command's raw argument string against this spec, printing any
     * problem through {@code output} and returning the typed values, or an
     * invalid result the command returns straight back. The whole-string entry
     * point for a command whose verb is not a separate token: it splits on
     * whitespace, with a null or blank string yielding no tokens. A command that
     * peels a leading verb first tokenizes itself and calls
     * {@link #parse(String[], CommandOutput)}.
     */
    public ParsedParameters parse(String args, CommandOutput output) {
        return parse(tokenize(args), output);
    }

    /**
     * Parses {@code tokens} - the command's parameter tokens, with any leading
     * verb already removed - against this spec, printing any problem through
     * {@code output} and returning the typed values, or an invalid result the
     * command returns straight back.
     */
    public ParsedParameters parse(String[] tokens, CommandOutput output) {
        return ParameterParser.parse(this, tokens, output);
    }

    // Parses against the live console binding, for callers that have not adopted
    // the output seam.
    public ParsedParameters parse(String[] tokens) {
        return parse(tokens, ConsoleCommandOutput.INSTANCE);
    }

    // Records a parameter that may be supplied positionally (in declaration
    // order) or by name, and returns its key.
    protected final <T> Parameter<T> acceptsPositional(String name, String valueHint,
            ValueParser<T> valueParser) {
        return record(Parameter.positional(name, valueHint, valueParser));
    }

    // Records a name-only parameter - one that never claims a positional slot -
    // and returns its key.
    protected final <T> Parameter<T> acceptsNamed(String name, String valueHint,
            ValueParser<T> valueParser) {
        return record(Parameter.named(name, valueHint, valueParser));
    }

    // Records a bare keyword flag - supplied as a lone token, never name=value or
    // a positional slot - and returns its key. The flag reads true when present
    // and false when omitted.
    protected final Parameter<Boolean> acceptsFlag(String name) {
        return record(Parameter.flag(name));
    }

    List<Parameter<?>> getParameters() {
        return parameters;
    }

    String getUsage() {
        return usage;
    }

    private <T> Parameter<T> record(Parameter<T> parameter) {
        parameters.add(parameter);
        return parameter;
    }

    // Splits a raw argument string into tokens on whitespace; a null or blank
    // string yields no tokens (rather than one empty token, which String.split
    // would return), so an argument-less command parses as nothing supplied.
    private static String[] tokenize(String args) {
        var trimmed = args == null ? "" : args.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }

    // The concrete empty spec behind takingNoArguments: it declares no
    // parameters, so the parser treats any supplied token as surplus.
    private static final class NoArgumentsSpec extends ParameterSpec {
        private NoArgumentsSpec(String usage) {
            super(usage);
        }
    }
}
