package kmlib.console.output;

import org.lazywizard.console.Console;

/**
 * Routes command output to the live Console Commands overlay. The production
 * binding of {@link CommandOutput}; concentrates the third-party {@code Console}
 * dependency in one place so the commands that consume the port never name it.
 */
public final class ConsoleCommandOutput implements CommandOutput {
    @Override
    public void showMessage(String message) {
        Console.showMessage(message);
    }
}
