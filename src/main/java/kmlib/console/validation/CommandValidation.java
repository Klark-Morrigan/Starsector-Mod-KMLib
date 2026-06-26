package kmlib.console.validation;

import com.fs.starfarer.api.Global;

import kmlib.starsector.systems.StarSystems;
import kmlib.text.KmlibStrings;

import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fluent precondition checks for KMLib's console commands. A command chains only
 * the guards it needs and validates once:
 *
 * <pre>
 * CommandValidationResult command = new CommandValidation(context, args)
 *         .inCampaign()
 *         .validateAndPrintFeedback();
 * if (!command.isValid()) {
 *     return command.getResult();
 * }
 * </pre>
 */
public final class CommandValidation {
    private final CommandContext context;
    private final String args;
    // Each check returns null when it passes, or the failure to report otherwise.
    private final List<Supplier<Failure>> checks = new ArrayList<>();

    public CommandValidation(CommandContext context, String args) {
        this.context = context;
        this.args = args;
    }

    public CommandValidation inCampaign() {
        checks.add(() -> context.isInCampaign()
                ? null
                : new Failure("This command can only run in a campaign.",
                        CommandResult.WRONG_CONTEXT));
        return this;
    }

    public CommandValidationResult validateAndPrintFeedback() {
        for (Supplier<Failure> check : checks) {
            var failure = check.get();
            if (failure != null) {
                Console.showMessage(failure.message);
                return CommandValidationResult.invalid(failure.result);
            }
        }
        return CommandValidationResult.valid();
    }

    // A failed check: the message to print and the result the command returns.
    private static final class Failure {
        private final String message;
        private final CommandResult result;

        private Failure(String message, CommandResult result) {
            this.message = message;
            this.result = result;
        }
    }
}
