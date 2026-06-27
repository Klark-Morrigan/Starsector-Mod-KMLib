package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.console.output.CommandOutput;
import kmlib.console.output.ConsoleCommandOutput;
import kmlib.console.validation.CommandValidation;
import kmlib.starsector.entities.Gates;
import kmlib.starsector.systems.StarSystems;

import org.lazywizard.console.BaseCommand;

/**
 * Console command (dev tool): activates the gate with the given id in the
 * current system. In-system only. Takes the target gate's id as its argument.
 *
 * <p>Resolves the gate by id within the current system and hands it to
 * {@link Gates#activateGate}, which owns the gate-state mechanics. Player
 * feedback goes through a {@link CommandOutput} seam so the command's outcome
 * branches stay independent of the live console sink.
 */
public final class ActivateGateCommand implements BaseCommand {
    private final CommandOutput output;

    // Console Commands instantiates a command through its no-arg constructor
    // (Class.newInstance), so that path wires the live console binding; the
    // second constructor accepts an explicit binding for callers that supply
    // their own.
    public ActivateGateCommand() {
        this(ConsoleCommandOutput.INSTANCE);
    }

    ActivateGateCommand(CommandOutput output) {
        this.output = output;
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        var command = new CommandValidation(context, args, output)
                .inCampaign()
                .inSystem()
                .hasArguments()
                .validateAndPrintFeedback();
        if (!command.isValid()) {
            return command.getResult();
        }

        var system = StarSystems.getPlayerStarSystem(Global.getSector());
        var id = args.trim();
        var gate = StarSystems.find(system, Tags.GATE, id);
        if (gate == null) {
            output.showMessage("No gate with id '" + id + "' in "
                    + system.getName() + ".");
            return CommandResult.ERROR;
        }
        Gates.activateGate(gate);

        output.showMessage("Activated gate '" + id + "' in "
                + system.getName() + "; it lights up on its next advance.");
        return CommandResult.SUCCESS;
    }
}
