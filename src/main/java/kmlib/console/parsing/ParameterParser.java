package kmlib.console.parsing;

import kmlib.console.output.CommandOutput;

import org.lazywizard.console.BaseCommand.CommandResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The parsing algorithm behind {@link ParameterSpec#parse}: it matches a
 * command's argument tokens to the spec's declared {@link Parameter}s and reports
 * any problem through the {@link CommandOutput} seam. Stateless - the spec it
 * works on is passed in - so the declaration ({@link ParameterSpec}) and the
 * algorithm stay separate responsibilities.
 *
 * <p>Each token is {@code name=value}, a bare flag keyword, or a bare positional.
 * A named token fills its parameter directly and drops it out of the positional
 * order; a bare token matching a declared flag name toggles that flag; any other
 * bare token fills the still-open positional slots in declared order. A name-only
 * parameter and a flag never take a slot. Because flags are matched first, a
 * positional value cannot itself be a declared flag's keyword. Per-parameter
 * typing is delegated to each parameter's {@link ValueParser}. An unknown name, a
 * flag given a value, a value its parser rejects, a missing required parameter,
 * or more positionals than open slots are each reported (yielding
 * {@code BAD_SYNTAX}).
 */
final class ParameterParser {
    private ParameterParser() {
    }

    static ParsedParameters parse(ParameterSpec spec, String[] tokens, CommandOutput output) {
        var parameters = spec.getParameters();
        var supplied = new HashMap<Parameter<?>, Object>();
        var positionals = new ArrayList<String>();
        for (var token : tokens) {
            var separator = token.indexOf('=');
            if (separator < 0) {
                // A bare token toggles a flag if it names one; any other bare
                // token is a positional value, filled later against the open slots.
                var parameter = findByName(parameters, token);
                if (parameter != null && parameter.isFlag()) {
                    supplied.put(parameter, Boolean.TRUE);
                } else {
                    positionals.add(token);
                }
                continue;
            }
            var name = token.substring(0, separator);
            var parameter = findByName(parameters, name);
            if (parameter == null) {
                output.showMessage("Unknown parameter '" + name + "'. Use "
                        + describeAcceptedParameters(parameters) + '.');
                return ParsedParameters.createInvalid(CommandResult.BAD_SYNTAX);
            }
            if (parameter.isFlag()) {
                // Naming a flag with a value is a distinct mistake from naming
                // something unknown, so it gets its own correction.
                output.showMessage("'" + name + "' is a flag; give it on its own,"
                        + " without '='.");
                return ParsedParameters.createInvalid(CommandResult.BAD_SYNTAX);
            }
            if (!storeValue(parameter, token.substring(separator + 1), supplied, output)) {
                return ParsedParameters.createInvalid(CommandResult.BAD_SYNTAX);
            }
        }

        // Fill the still-open positional slots, in declared order, from the bare
        // tokens; a name-only or already-named parameter is not a slot.
        var openSlots = new ArrayList<Parameter<?>>();
        for (var parameter : parameters) {
            if (parameter.isPositional() && !supplied.containsKey(parameter)) {
                openSlots.add(parameter);
            }
        }
        if (positionals.size() > openSlots.size()) {
            var usage = spec.getUsage();
            output.showMessage("Too many arguments." + (usage == null ? "" : " " + usage));
            return ParsedParameters.createInvalid(CommandResult.BAD_SYNTAX);
        }
        for (var index = 0; index < positionals.size(); index++) {
            if (!storeValue(openSlots.get(index), positionals.get(index), supplied, output)) {
                return ParsedParameters.createInvalid(CommandResult.BAD_SYNTAX);
            }
        }

        for (var parameter : parameters) {
            if (parameter.isRequired() && !supplied.containsKey(parameter)) {
                output.showMessage("Missing required parameter '" + parameter.getName() + "'.");
                return ParsedParameters.createInvalid(CommandResult.BAD_SYNTAX);
            }
        }
        return ParsedParameters.createValid(supplied);
    }

    // Parses raw for parameter and records it, or prints why it was rejected and
    // returns false (so the caller can stop with BAD_SYNTAX). Framing the message
    // here keeps every malformed value reading the same way.
    private static boolean storeValue(Parameter<?> parameter, String raw,
            Map<Parameter<?>, Object> supplied, CommandOutput output) {
        try {
            supplied.put(parameter, parameter.parseValue(raw));
            return true;
        } catch (ValueParseException malformed) {
            output.showMessage("Invalid " + parameter.getName() + " '" + raw
                    + "'. Expected " + malformed.getMessage() + '.');
            return false;
        }
    }

    // The parameter named by key, case-insensitively, or null when none matches;
    // the caller classifies the match (flag vs value) by its kind.
    private static Parameter<?> findByName(List<Parameter<?>> parameters, String key) {
        for (var parameter : parameters) {
            if (parameter.getName().equalsIgnoreCase(key)) {
                return parameter;
            }
        }
        return null;
    }

    // The accepted parameters - value parameters as name=<hint> pairs, flags as
    // bare keywords - for telling the player which names exist when they used one
    // that does not.
    private static String describeAcceptedParameters(List<Parameter<?>> parameters) {
        var description = new StringBuilder();
        for (var parameter : parameters) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append(parameter.getName());
            if (!parameter.isFlag()) {
                description.append('=').append(parameter.getValueHint());
            }
        }
        return description.toString();
    }
}
