package kmlib.mods.console.commands.parsing;

import kmlib.mods.console.ConsoleStrings;
import kmlib.mods.console.commands.output.CommandOutput;

import org.lazywizard.console.BaseCommand.CommandResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static kmlib.starsector.strings.KmlibStringKeys.CATEGORY;
import static kmlib.starsector.strings.KmlibStringKeys.CONSOLE_PARAMETER_FLAG_GIVEN_VALUE;
import static kmlib.starsector.strings.KmlibStringKeys.CONSOLE_PARAMETER_INVALID_VALUE;
import static kmlib.starsector.strings.KmlibStringKeys.CONSOLE_PARAMETER_MISSING;
import static kmlib.starsector.strings.KmlibStringKeys.CONSOLE_PARAMETER_STANDALONE_COMBINED;
import static kmlib.starsector.strings.KmlibStringKeys.CONSOLE_PARAMETER_TOO_MANY;
import static kmlib.starsector.strings.KmlibStringKeys.CONSOLE_PARAMETER_UNKNOWN;

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
 * flag given a value, a value its parser rejects, more positionals than open
 * slots, a standalone parameter given with anything else, or a missing required
 * parameter are each reported (yielding {@code BAD_SYNTAX}). A standalone
 * parameter given alone is the whole invocation, so nothing else is required of it.
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

                return failSyntax(
                    output,
                    ConsoleStrings.format(
                        CATEGORY,
                        CONSOLE_PARAMETER_UNKNOWN,
                        name,
                        ParameterUsage.describeAcceptedParameters(parameters)));
            }

            if (parameter.isFlag()) {

                // Naming a flag with a value is a distinct mistake from naming
                // something unknown, so it gets its own correction.
                return failSyntax(
                    output,
                    ConsoleStrings.format(CATEGORY, CONSOLE_PARAMETER_FLAG_GIVEN_VALUE, name));
            }

            if (!storeValue(parameter, token.substring(separator + 1), supplied, output)) {
                return failSyntax();
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
            return failSyntax(
                output,
                ConsoleStrings.format(CATEGORY, CONSOLE_PARAMETER_TOO_MANY, spec.describeUsage()));
        }

        for (var index = 0; index < positionals.size(); index++) {

            var isStored = storeValue(openSlots.get(index), positionals.get(index), supplied, output);
            if (!isStored) {
                return failSyntax();
            }
        }

        var standalone = findSuppliedStandalone(parameters, supplied);
        if (standalone != null) {

            if (supplied.size() > 1) {

                return failSyntax(
                    output,
                    ConsoleStrings.format(
                        CATEGORY,
                        CONSOLE_PARAMETER_STANDALONE_COMBINED,
                        standalone.getName(),
                        spec.describeUsage()));
            }
            return ParsedParameters.createValid(supplied);
        }

        for (var parameter : parameters) {

            if (parameter.isRequired() && !supplied.containsKey(parameter)) {

                return failSyntax(
                    output,
                    ConsoleStrings.format(CATEGORY, CONSOLE_PARAMETER_MISSING, parameter.getName()));
            }
        }

        return ParsedParameters.createValid(supplied);
    }

    // Stops the parse on a malformed argument, for a value that has already printed its own
    // correction. Every way of being malformed ends the same way, so the ending is written once
    // rather than at each of the places that can reach it.
    private static ParsedParameters failSyntax() {

        return ParsedParameters.createInvalid(CommandResult.BAD_SYNTAX);
    }

    // The same stop, preceded by the one correction naming what was wrong with the argument.
    private static ParsedParameters failSyntax(CommandOutput output, String message) {

        output.showMessage(message);
        return failSyntax();
    }

    // Parses raw for parameter and records it, or prints why it was rejected and
    // returns false (so the caller can stop with BAD_SYNTAX). Framing the message
    // here keeps every malformed value reading the same way.
    private static boolean storeValue(
            Parameter<?> parameter,
            String raw,
            Map<Parameter<?>, Object> supplied,
            CommandOutput output) {

        try {
            supplied.put(parameter, parameter.parseValue(raw));
            return true;

        } catch (ValueParseException valueParseException) {

            output.showMessage(ConsoleStrings.format(
                CATEGORY,
                CONSOLE_PARAMETER_INVALID_VALUE,
                parameter.getName(),
                raw,
                valueParseException.getMessage()));

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

    // The first standalone parameter the player supplied, or null when they supplied none.
    private static Parameter<?> findSuppliedStandalone(
            List<Parameter<?>> parameters,
            Map<Parameter<?>, Object> supplied) {

        for (var parameter : parameters) {

            if (parameter.isStandalone() && supplied.containsKey(parameter)) {
                return parameter;
            }
        }
        return null;
    }
}
