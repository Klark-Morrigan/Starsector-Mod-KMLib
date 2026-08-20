package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.console.input.CommandInput;
import kmlib.console.output.CommandOutput;
import kmlib.console.output.ConsoleCommandOutput;

import org.lazywizard.console.BaseCommand;

/**
 * Base for KMLib's console commands: it owns the player-output seam every command
 * reports through and wires a per-invocation {@link CommandInput} bound to that
 * seam, so a concrete command declares only its two constructors and then states
 * its context guards and parameter spec.
 *
 * <p>Console Commands instantiates a command through its public no-arg
 * constructor (Class.newInstance), which binds the live console; the
 * output-injecting constructor lets a test - or any caller - supply its own sink.
 * A command then reads its input with {@link #readInput}; one whose flow branches
 * between the context check and the parse reaches for the
 * {@link kmlib.console.validation.CommandContextValidation} and
 * {@link kmlib.console.parsing.ParameterSpec} collaborators directly, using the
 * inherited {@link #output}.
 */
public abstract class KmlibBaseConsoleCommand implements BaseCommand {
    protected final CommandOutput output;

    protected KmlibBaseConsoleCommand() {
        this(ConsoleCommandOutput.INSTANCE);
    }

    protected KmlibBaseConsoleCommand(CommandOutput output) {
        this.output = output;
    }

    // A CommandInput for this invocation, pre-bound to the command's output seam,
    // so a subclass adds only its guards and spec before parsing.
    protected final CommandInput readInput(CommandContext context, String args) {
        return new CommandInput(context, args, output);
    }

    /**
     * The sector this run acts in, which every command reads here rather than reaching for the
     * game's own.
     *
     * <p>Today the game has exactly one sector and this is it. That is not a permanent fact about
     * Starsector: additional sectors, switched between during a campaign, are a standing ambition
     * in the modding community, and a console command is precisely the surface that would then
     * have to act in whichever one the player is currently in rather than in "the" sector.
     *
     * <p>Read through one named method so that day is one edit here instead of one per command.
     * Nothing below a command has to change with it - the reads and operations this library
     * offers already take the sector as a parameter, so a command is where the choice is made and
     * the only place that knows how it was made.
     *
     * @return the sector to act in; null outside a running game, which the context guards are what
     *         keep a command from reaching
     */
    protected final SectorAPI readActiveSector() {
        return Global.getSector();
    }
}
