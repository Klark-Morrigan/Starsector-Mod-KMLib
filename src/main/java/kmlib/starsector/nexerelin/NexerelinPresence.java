package kmlib.starsector.nexerelin;

import kmlib.starsector.settings.ModPresence;

/**
 * Whether Nexerelin is enabled this run - the gate every read of that mod stands behind, and the
 * one place its mod id is written.
 *
 * <p>The id has a home of its own rather than sitting at each gate because the gate is load-bearing
 * here in a way a settings read is not: what stands behind it is a class naming a Nexerelin type,
 * which an install without the mod cannot resolve. A mod that renames its id is then one edit
 * rather than one per routine that defers to it.
 *
 * <p>Public because that argument does not stop at this library. A mod is asked about by whoever
 * integrates with it, and every consumer spelling the id out for itself is the same drift with more
 * places to look - so the gate is offered rather than kept, and a caller reaching for Nexerelin
 * asks here instead of writing the hop again.
 */
public final class NexerelinPresence {

    private static final String MOD_ID = "nexerelin";

    private NexerelinPresence() {
        // utility class, no instances.
    }

    /**
     * @return true when the game is up and reports Nexerelin enabled; false in every other state,
     *         which is the answer that leaves a caller behaving as it does without the mod
     */
    public static boolean isModEnabled() {
        return ModPresence.isModEnabled(MOD_ID);
    }
}
