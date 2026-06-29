package kmlib.console;

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
public abstract class BaseConsoleCommand implements BaseCommand {
    protected final CommandOutput output;

    protected BaseConsoleCommand() {
        this(ConsoleCommandOutput.INSTANCE);
    }

    protected BaseConsoleCommand(CommandOutput output) {
        this.output = output;
    }

    // A CommandInput for this invocation, pre-bound to the command's output seam,
    // so a subclass adds only its guards and spec before parsing.
    protected final CommandInput readInput(CommandContext context, String args) {
        return new CommandInput(context, args, output);
    }
}
