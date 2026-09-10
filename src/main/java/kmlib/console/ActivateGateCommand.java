package kmlib.console;

import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.console.output.CommandOutput;
import kmlib.console.parsing.Parameter;
import kmlib.console.parsing.ParameterSpec;
import kmlib.starsector.entities.Gates;
import kmlib.starsector.systems.SectorStarSystems;
import kmlib.starsector.systems.StarSystems;

import static kmlib.console.parsing.ParameterValues.text;

/**
 * Console command (dev tool): activates the gate with the given id in the
 * current system. In-system only. Takes the target gate's id as its argument.
 *
 * <p>Resolves the gate by id within the current system and hands it to
 * {@link Gates#activateGate}, which owns the gate-state mechanics. Player
 * feedback goes through the inherited {@link CommandOutput} seam so the command's
 * outcome branches stay independent of the live console sink.
 */
public final class ActivateGateCommand extends KmlibBaseConsoleCommand {
    private static final ActivateGateSpec SPEC = new ActivateGateSpec();

    public ActivateGateCommand() {
    }

    ActivateGateCommand(CommandOutput output) {
        super(output);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        // Context guards run first; the id is then required, so a missing or
        // surplus argument is reported as bad syntax before any gate lookup.
        var parsed = readInput(context, args)
            .requireCampaign()
            .requireStarSystem(readActiveSector())
            .parseArguments(SPEC);
        if (!parsed.isValid()) {
            return parsed.getResult();
        }

        var system = SectorStarSystems.getPlayerStarSystem(readActiveSector());
        var id = parsed.get(SPEC.id);
        var gate = StarSystems.find(system, Tags.GATE, id);
        if (gate == null) {
            output.showMessage("No gate with id '"
                + id
                + "' in "
                + StarSystems.readDisplayName(system)
                + ".");
            return CommandResult.ERROR;
        }
        Gates.activateGate(readActiveSector(), gate);

        output.showMessage("Activated gate '"
            + id
            + "' in "
            + StarSystems.readDisplayName(system)
            + "; it lights up on its next advance.");
        return CommandResult.SUCCESS;
    }

    /**
     * What {@code kmlib_activate_gate} accepts: the id of the gate to activate,
     * required so an empty invocation is rejected as bad syntax rather than
     * searching for a blank id.
     */
    private static final class ActivateGateSpec extends ParameterSpec {
        private final Parameter<String> id =
            acceptsPositional("id", "<gate-id>", text()).markRequired();

        private ActivateGateSpec() {
            super("Usage: kmlib_activate_gate <id>.");
        }
    }
}
