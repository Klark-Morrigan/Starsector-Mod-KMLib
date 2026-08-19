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
 */
final class NexerelinPresence {

    private static final String MOD_ID = "nexerelin";

    private NexerelinPresence() {
        // utility class, no instances.
    }

    static boolean isModEnabled() {
        return ModPresence.isModEnabled(MOD_ID);
    }
}
