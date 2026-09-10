package kmlib.mods.console.commands.validation;

import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.mods.console.commands.output.CommandOutput;
import kmlib.mods.console.commands.output.ConsoleCommandOutput;
import kmlib.starsector.systems.SectorStarSystems;

import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fluent run-context checks for KMLib's console commands: whether the command may
 * run given the current game state (in a campaign, inside a star system). A
 * command chains only the guards it needs and validates once:
 *
 * <pre>
 * CommandValidationResult command = new CommandContextValidation(context, output)
 *     .requireCampaign()
 *     .requireStarSystem(sector)
 *     .validateAndPrintFeedback();
 * if (!command.isValid()) {
 *     return command.getResult();
 * }
 * </pre>
 *
 * <p>This owns only the context guards; argument shape is the parser's job
 * ({@link kmlib.mods.console.commands.parsing.ParameterSpec}). The common command runs both in
 * sequence, for which {@link kmlib.mods.console.commands.CommandInput} composes the two into a
 * single step; a command direct-uses this only when its flow branches between the
 * context check and the parse (e.g. it inspects a leading token first).
 *
 * <p>Built from only the context a command has at runtime; Console Commands does
 * not hand a command its own name, so messages are phrased generically.
 * {@link #validateAndPrintFeedback()} runs the chain in order, stops at the first
 * failure, reports it through the {@link CommandOutput} seam, and returns the
 * result to return ({@code WRONG_CONTEXT} for a context guard); passing all
 * checks yields a valid result.
 */
public final class CommandContextValidation {
    private final CommandContext context;
    private final CommandOutput output;
    // Each check returns null when it passes, or the failure to report otherwise.
    private final List<Supplier<Failure>> checks = new ArrayList<>();

    // Callers that have not adopted the output seam get the live console binding.
    public CommandContextValidation(CommandContext context) {
        this(context, ConsoleCommandOutput.INSTANCE);
    }

    public CommandContextValidation(CommandContext context, CommandOutput output) {
        this.context = context;
        this.output = output;
    }

    public CommandContextValidation requireCampaign() {
        checks.add(() -> context.isInCampaign()
            ? null
            : new Failure(
                "This command can only run in a campaign.",
                CommandResult.WRONG_CONTEXT));
        return this;
    }

    /**
     * Requires the player to be inside a star system of {@code sector}, which the caller states
     * rather than this guard reading it: which sector a command acts in is the command's own.
     *
     * @param sector the sector to look for the player in; null reads as not being in a system,
     *               which is the answer a run outside a campaign wants
     * @return this, so guards chain
     */
    public CommandContextValidation requireStarSystem(SectorAPI sector) {
        checks.add(() -> SectorStarSystems.getPlayerStarSystem(sector) != null
            ? null
            : new Failure(
                "This command must be run inside a star system.",
                CommandResult.WRONG_CONTEXT));
        return this;
    }

    public CommandValidationResult validateAndPrintFeedback() {
        for (Supplier<Failure> check : checks) {
            var failure = check.get();
            if (failure != null) {
                output.showMessage(failure.message);
                return CommandValidationResult.createInvalid(failure.result);
            }
        }
        return CommandValidationResult.createValid();
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
