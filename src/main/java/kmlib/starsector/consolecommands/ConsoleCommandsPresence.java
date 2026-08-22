package kmlib.starsector.consolecommands;

import kmlib.starsector.settings.ModPresence;

/**
 * Whether Console Commands is enabled this run.
 */
public final class ConsoleCommandsPresence {

    public static final String MOD_ID = "lw_console";

    private ConsoleCommandsPresence() { // utility class, no instances.
    }

    /**
     * @return whether Console Commands is enabled this run; false before the game has stood its
     *         mod set up, which is {@link ModPresence}'s answer rather than this one's
     */
    public static boolean isModEnabled() {
        return ModPresence.isModEnabled(MOD_ID);
    }
}
