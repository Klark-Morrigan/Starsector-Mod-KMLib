package kmlib.console.parsing;

/**
 * One parameter in a command's spec: its name, the compact value hint shown in
 * usage and unknown-parameter messages (e.g. {@code <id>}), how its raw token
 * becomes a typed value, and whether it may be given positionally, is a bare
 * keyword flag, is required, or carries a default. Declared as a field of a
 * {@link ParameterSpec} through its {@code acceptsX} factories; that same
 * instance is the key a command uses to read its value back from
 * {@link ParsedParameters}, so retrieval stays type safe with no casts at the
 * call site.
 *
 * <p>A flag is the degenerate case that carries no value: it has no value parser
 * or hint, is supplied as a bare keyword rather than {@code name=value}, and
 * reads {@link Boolean#TRUE true} when present and false (its default) when not.
 */
public final class Parameter<T> {
    private final String name;
    private final String valueHint;
    private final boolean positional;
    private final ValueParser<T> valueParser;
    private boolean flag;
    private boolean required;
    private T defaultValue;

    private Parameter(
            String name,
            String valueHint,
            boolean positional,
            ValueParser<T> valueParser) {
        this.name = name;
        this.valueHint = valueHint;
        this.positional = positional;
        this.valueParser = valueParser;
    }

    // Marks the parameter as required: parsing fails if it is never supplied.
    public Parameter<T> markRequired() {
        this.required = true;
        return this;
    }

    // Sets the value returned when the parameter is not supplied; intended as the
    // alternative to markRequired.
    public Parameter<T> defaultsTo(T value) {
        this.defaultValue = value;
        return this;
    }

    // A parameter that may be supplied positionally (in declared order) or by
    // name. Created through ParameterSpec#acceptsPositional, which records it.
    static <T> Parameter<T> positional(
            String name,
            String valueHint,
            ValueParser<T> valueParser) {
        return new Parameter<>(name, valueHint, true, valueParser);
    }

    // A name-only parameter: it never claims a positional slot, so it must be
    // given as name=value. Created through ParameterSpec#acceptsNamed.
    static <T> Parameter<T> named(
            String name,
            String valueHint,
            ValueParser<T> valueParser) {
        return new Parameter<>(name, valueHint, false, valueParser);
    }

    // A bare keyword flag: it carries no value, so it has no parser or hint and
    // never claims a positional slot; present reads true, absent reads its false
    // default. Created through ParameterSpec#acceptsFlag.
    static Parameter<Boolean> flag(String name) {
        Parameter<Boolean> parameter = new Parameter<>(name, null, false, null);
        parameter.flag = true;
        parameter.defaultValue = Boolean.FALSE;
        return parameter;
    }

    String getName() {
        return name;
    }

    String getValueHint() {
        return valueHint;
    }

    boolean isPositional() {
        return positional;
    }

    boolean isFlag() {
        return flag;
    }

    boolean isRequired() {
        return required;
    }

    T getDefaultValue() {
        return defaultValue;
    }

    // Converts a raw token to this parameter's typed value, propagating the
    // ValueParseException the ParameterParser frames for the player.
    T parseValue(String raw) throws ValueParseException {
        return valueParser.parseValue(raw);
    }
}
