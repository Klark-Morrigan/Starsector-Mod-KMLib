package kmlib.mods.console.commands.parsing;

import kmlib.mods.console.commands.output.CommandOutput;
import kmlib.mods.console.commands.output.ConsoleCommandOutput;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A console command's parameter specification: the command's name and the ordered parameters it
 * accepts, declared as one cohesive unit. A command subclasses this and declares each parameter as
 * a field through {@link #acceptsPositional} / {@link #acceptsNamed} / {@link #acceptsKeyword} /
 * {@link #acceptsFlag}, so the spec reads top to bottom as "what this command accepts" and those
 * fields double as the type-safe keys for reading values back from {@link ParsedParameters}:
 *
 * <pre>
 * private static final class SpawnSpec extends ParameterSpec {
 *     final Parameter&lt;String&gt; focus = acceptsPositional("focus", "&lt;id&gt;", text());
 *     final Parameter&lt;Float&gt;  speed = acceptsPositional("speed", "&lt;deg/day&gt;", decimal(...));
 *     SpawnSpec() { super("kmlib_spawn", "&lt;kind&gt;"); }
 * }
 * </pre>
 *
 * <p>Each {@code acceptsX} call both records the parameter - in declaration order, which is the
 * positional order - and returns its key, so a command never registers a parameter separately from
 * declaring it. The usage line is composed from those same declarations, so a command states its
 * grammar once. Parsing is delegated to {@link ParameterParser}: a command calls {@link #parse} and
 * reads values back by the field keys.
 */
public abstract class ParameterSpec {

    private final List<Parameter<?>> parameters = new ArrayList<>();
    private final String usageHead;

    /**
     * Declares the spec of the command the console knows by {@code commandName}.
     *
     * @param commandName the one word the console knows the command by
     * @throws IllegalArgumentException when the name is blank or holds more than one word
     */
    protected ParameterSpec(String commandName) {
        this.usageHead = requireOneWord(commandName);
    }

    /**
     * Declares the spec of a command that reads a leading word itself before parsing the rest,
     * such as the kind of thing to spawn.
     *
     * @param commandName     the one word the console knows the command by
     * @param leadingWordHint how the usage line names that word, such as {@code <kind>}
     * @throws IllegalArgumentException when the name is blank or holds more than one word
     */
    protected ParameterSpec(String commandName, String leadingWordHint) {
        this.usageHead = requireOneWord(commandName) + ' ' + leadingWordHint;
    }

    /**
     * A spec that accepts no parameters - for a command that takes no arguments but still wants a
     * stray one reported as bad syntax rather than silently ignored. Spares such a command an empty
     * subclass.
     *
     * @param commandName the one word the console knows the command by
     * @return a spec rejecting any argument
     * @throws IllegalArgumentException when the name is blank or holds more than one word
     */
    public static ParameterSpec takingNoArguments(String commandName) {
        return new NoArgumentsSpec(commandName);
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
    protected final <T> Parameter<T> acceptsPositional(
            String name,
            String valueHint,
            ValueParser<T> valueParser) {

        return record(Parameter.positional(name, valueHint, valueParser));
    }

    // Records a positional parameter whose value is one of a fixed set of words, each standing
    // for the value it selects, and returns its key. The words, in the map's order, are both the
    // usage hint and the choices a stray word is answered with.
    protected final <T> Parameter<T> acceptsKeyword(String name, Map<String, T> valuesByKeyword) {

        var valueHint = String.join("|", valuesByKeyword.keySet());

        return acceptsPositional(name, valueHint, ParameterValues.oneOfKeywords(valuesByKeyword));
    }

    // Records a name-only parameter - one that never claims a positional slot -
    // and returns its key.
    protected final <T> Parameter<T> acceptsNamed(
            String name,
            String valueHint,
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

    // Composed on each call rather than at construction, because a subclass's parameter fields
    // are initialised only after this constructor has returned.
    String describeUsage() {
        return ParameterUsage.describeUsage(usageHead, parameters);
    }

    private <T> Parameter<T> record(Parameter<T> parameter) {

        parameters.add(parameter);
        return parameter;
    }

    // A name with a space in it is a whole usage line passed where the name now goes, which would
    // print as "Usage: Usage: ...". Refused when the spec is built, so it fails at load rather than
    // the first time a player mistypes the command.
    private static String requireOneWord(String commandName) {

        var words = KmlibStrings.splitIntoWords(commandName);
        if (words.size() != 1 || !words.get(0).equals(commandName)) {

            throw new IllegalArgumentException(
                "A command name is one word, not '" + commandName + "'");
        }
        return commandName;
    }

    // The words of a raw argument string, as the tokens the parser walks. Taken
    // from the shared word read, which answers no words for a null or blank
    // string rather than the one empty piece a bare split leaves - so an
    // argument-less command parses as nothing supplied.
    private static String[] tokenize(String args) {
        return KmlibStrings.splitIntoWords(args).toArray(new String[0]);
    }

    // The concrete empty spec behind takingNoArguments: it declares no
    // parameters, so the parser treats any supplied token as surplus.
    private static final class NoArgumentsSpec extends ParameterSpec {

        private NoArgumentsSpec(String commandName) {
            super(commandName);
        }
    }
}
