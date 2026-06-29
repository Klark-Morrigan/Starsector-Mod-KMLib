package kmlib.console.input;

import kmlib.console.output.CommandOutput;
import kmlib.console.parsing.ParameterSpec;
import kmlib.console.parsing.ParsedParameters;
import kmlib.console.validation.CommandContextValidation;

import org.lazywizard.console.BaseCommand.CommandContext;

/**
 * A console command's raw input - its run context and argument string - taken
 * through the two checks every command makes before its work: the run-context
 * guards, then the argument parse. It composes {@link CommandContextValidation}
 * and {@link ParameterSpec} so a command states its requirements and parses in
 * one fluent step, keeping a single guard-and-return:
 *
 * <pre>
 * ParsedParameters run = new CommandInput(context, args, output)
 *         .requireCampaign()
 *         .requireStarSystem()
 *         .parseArguments(SPEC);
 * if (!run.isValid()) {
 *     return run.getResult();
 * }
 * String id = run.get(SPEC.id);
 * </pre>
 *
 * <p>Context is checked first, so a wrong-context run reports
 * {@code WRONG_CONTEXT} without the tokens ever being read; only once it passes
 * are the arguments parsed (a malformed one yielding {@code BAD_SYNTAX}). Both
 * outcomes - and the success carrying the typed values - come back as one
 * {@link ParsedParameters}. The guards and the parser stay separate
 * collaborators; this only sequences them, so a command whose flow branches
 * between the two uses those collaborators directly instead.
 */
public final class CommandInput {
    private final CommandContextValidation contextValidation;
    private final String args;
    private final CommandOutput output;

    public CommandInput(CommandContext context, String args, CommandOutput output) {
        this.contextValidation = new CommandContextValidation(context, output);
        this.args = args;
        this.output = output;
    }

    public CommandInput requireCampaign() {
        contextValidation.requireCampaign();
        return this;
    }

    public CommandInput requireStarSystem() {
        contextValidation.requireStarSystem();
        return this;
    }

    /**
     * Runs the requested context guards, then parses the argument string against
     * {@code spec}. Returns the context failure ({@code WRONG_CONTEXT}) when a
     * guard fails - having reported it - otherwise the parse outcome.
     */
    public ParsedParameters parseArguments(ParameterSpec spec) {
        var contextResult = contextValidation.validateAndPrintFeedback();
        if (!contextResult.isValid()) {
            return ParsedParameters.createInvalid(contextResult.getResult());
        }
        return spec.parse(args, output);
    }
}
