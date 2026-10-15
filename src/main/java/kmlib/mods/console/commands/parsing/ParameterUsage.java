package kmlib.mods.console.commands.parsing;

import java.util.ArrayList;
import java.util.List;

/**
 * The player-facing text naming a spec's parameters: the usage line a malformed run is shown, and
 * the list of accepted names an unknown one is answered with.
 *
 * <p>Composed from the declared parameters rather than written per command, so what a command says
 * it accepts cannot drift from what its parser accepts.
 *
 * <p>A placeholder hint is written in angle brackets and a keyword choice bare, so
 * {@code [<entity-id>]} asks for a value and {@code [tree|flat|walks]} for one of those words. The
 * hint is the parameter's own, so the brackets are the declaring command's to write.
 */
final class ParameterUsage {

    private static final String ALTERNATIVE_FORM_SEPARATOR = ", or ";
    private static final String USAGE_PREFIX = "Usage: ";

    private ParameterUsage() {
    }

    // The usage line: the command's main form, then each standalone parameter as a form of its
    // own, since it is a whole invocation rather than one more option of the main one.
    static String describeUsage(String usageHead, List<Parameter<?>> parameters) {

        var mainForm = new StringBuilder(usageHead);
        var forms = new ArrayList<String>();

        for (var parameter : parameters) {

            if (parameter.isStandalone()) {

                forms.add(usageHead + ' ' + describeBareToken(parameter));
                continue;
            }

            mainForm
                .append(' ')
                .append(describeSlot(parameter));
        }

        forms.add(0, mainForm.toString());

        return USAGE_PREFIX + String.join(ALTERNATIVE_FORM_SEPARATOR, forms) + '.';
    }

    // The accepted parameters - value parameters as name=<hint> pairs, flags as bare keywords -
    // for telling the player which names exist when they used one that does not.
    static String describeAcceptedParameters(List<Parameter<?>> parameters) {

        var descriptions = new ArrayList<String>();

        for (var parameter : parameters) {
            descriptions.add(describeNamedToken(parameter));
        }

        return String.join(", ", descriptions);
    }

    // A parameter as its place in the main form shows it: bracketed unless it has to be given.
    private static String describeSlot(Parameter<?> parameter) {

        var token = describeBareToken(parameter);

        return parameter.isRequired()
            ? token
            : "[" + token + "]";
    }

    // A parameter as the player types it in its shortest form: a flag as its keyword, a
    // positional as its hint, and a name-only parameter as name=<hint>.
    private static String describeBareToken(Parameter<?> parameter) {

        return parameter.isPositional()
            ? parameter.getValueHint()
            : describeNamedToken(parameter);
    }

    private static String describeNamedToken(Parameter<?> parameter) {

        return parameter.isFlag()
            ? parameter.getName()
            : parameter.getName() + '=' + parameter.getValueHint();
    }
}
