package kmlib.console.validation;

import org.lazywizard.console.BaseCommand.CommandResult;

/**
 * The outcome of a {@link CommandContextValidation}: whether the command may
 * proceed, and - when it may not - the {@link CommandResult} the command should
 * return. Lets a command guard with one check and one return:
 * {@code if (!result.isValid()) return result.getResult();}.
 */
public final class CommandValidationResult {
    private final boolean valid;
    private final CommandResult commandResult;

    private CommandValidationResult(boolean valid, CommandResult commandResult) {
        this.valid = valid;
        this.commandResult = commandResult;
    }

    public boolean isValid() {
        return valid;
    }

    /**
     * @return the result to return from the command; meaningful only when
     *         {@link #isValid()} is false
     */
    public CommandResult getResult() {
        return commandResult;
    }

    static CommandValidationResult createValid() {
        return new CommandValidationResult(true, null);
    }

    static CommandValidationResult createInvalid(CommandResult commandResult) {
        return new CommandValidationResult(false, commandResult);
    }
}
