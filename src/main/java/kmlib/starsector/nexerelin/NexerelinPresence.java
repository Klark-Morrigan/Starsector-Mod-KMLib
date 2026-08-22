package kmlib.starsector.nexerelin;

import kmlib.starsector.settings.ModPresence;

/**
 * Whether Nexerelin is enabled this run.
 */
public final class NexerelinPresence {

    public static final String MOD_ID = "nexerelin";

    private NexerelinPresence() { // utility class, no instances.
    }

    /**
     * @return true when the game is up and reports Nexerelin enabled; false in every other state,
     *         which is the answer that leaves a caller behaving as it does without the mod
     */
    public static boolean isModEnabled() {
        return ModPresence.isModEnabled(MOD_ID);
    }
}
