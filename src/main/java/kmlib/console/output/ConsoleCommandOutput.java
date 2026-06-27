package kmlib.console.output;

import org.lazywizard.console.Console;

/**
 * Routes command output to the live Console Commands overlay. The production
 * binding of {@link CommandOutput}; concentrates the third-party {@code Console}
 * dependency in one place so the commands that consume the port never name it.
 *
 * <p>A single {@link #INSTANCE}: the binding is a stateless forwarder over the
 * static {@code Console} sink, so one shared value serves every command rather
 * than a fresh object per construction (the same enum-singleton shape KMLib uses
 * for the live sources backing its other ports).
 */
public enum ConsoleCommandOutput implements CommandOutput {
    INSTANCE;

    @Override
    public void showMessage(String message) {
        Console.showMessage(message);
    }
}
