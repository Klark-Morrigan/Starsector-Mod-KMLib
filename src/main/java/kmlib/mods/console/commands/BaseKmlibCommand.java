package kmlib.mods.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.mods.console.commands.input.CommandInput;
import kmlib.mods.console.commands.output.CommandOutput;
import kmlib.mods.console.commands.output.ConsoleCommandOutput;

import org.lazywizard.console.BaseCommand;

/**
 * Base for KMLib's console commands: it owns the player-output seam every command
 * reports through and wires a per-invocation {@link CommandInput} bound to that
 * seam, so a concrete command declares only its two constructors and then states
 * its context guards and parameter spec.
 *
 * <p>Console Commands instantiates a command through its public no-arg
 * constructor (Class.newInstance), which binds the live console; the
 * output-injecting constructor lets any caller supply its own sink instead.
 * A command then reads its input with {@link #readInput}; one whose flow branches
 * between the context check and the parse reaches for the
 * {@link kmlib.mods.console.commands.validation.CommandContextValidation} and
 * {@link kmlib.mods.console.commands.parsing.ParameterSpec} collaborators directly, using the
 * inherited {@link #output}.
 */
public abstract class BaseKmlibCommand implements BaseCommand {
    protected final CommandOutput output;

    protected BaseKmlibCommand() {
        this(ConsoleCommandOutput.INSTANCE);
    }

    protected BaseKmlibCommand(CommandOutput output) {
        this.output = output;
    }

    // A CommandInput for this invocation, pre-bound to the command's output seam,
    // so a subclass adds only its guards and spec before parsing.
    protected final CommandInput readInput(CommandContext context, String args) {
        return new CommandInput(context, args, output);
    }

    /**
     * The sector this run acts in.
     *
     * <p>Which sector a command acts in is decided here rather than at each call site, so a
     * command passes it down to reads and operations that all take it as a parameter.
     *
     * @return the sector to act in; null outside a running game, which the context guards keep a
     *         command from reaching
     */
    protected final SectorAPI readActiveSector() {
        return Global.getSector();
    }
}
