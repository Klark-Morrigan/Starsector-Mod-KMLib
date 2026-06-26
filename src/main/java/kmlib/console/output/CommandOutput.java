package kmlib.console.output;

/**
 * Player-facing output port for KMLib's console commands. Commands depend on
 * this role rather than the global console sink directly, so a command's outcome
 * logic is decoupled from the live overlay - which only functions inside a
 * running game. {@link ConsoleCommandOutput} is the binding that routes to the
 * console at runtime.
 */
public interface CommandOutput {
    void showMessage(String message);
}
