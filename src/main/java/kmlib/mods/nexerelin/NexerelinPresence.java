package kmlib.mods.nexerelin;

import kmlib.starsector.settings.modmanager.ModPresence;

/**
 * Whether Nexerelin is enabled this run.
 */
public final class NexerelinPresence {

    public static final String MOD_ID = "nexerelin";

    /**
     * The mod's name as a reader knows it: what a line about which mod is doing the work names, and
     * what a report about this integration having stopped holding is headed with.
     *
     * <p>Beside the ID rather than at each of the places that show it, so a record latched under the
     * ID and a report naming the mod cannot drift into two mods.
     */
    public static final String MOD_NAME = "Nexerelin";

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
