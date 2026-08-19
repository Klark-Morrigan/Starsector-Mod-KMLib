package kmlib.starsector.nexerelin;

import com.fs.starfarer.api.Global;

/**
 * Whether Nexerelin is enabled this run - the gate every read of that mod stands behind, and the
 * one place its mod id is written.
 *
 * <p>Shared rather than repeated because each routine in this package defers to Nexerelin for its
 * own reasons and would drift apart, and because the gate is load-bearing here in a way a settings
 * read is not: what stands behind it is a class naming a Nexerelin type, which an install without
 * the mod cannot resolve. A mod that renames its id is then one edit rather than one per routine.
 *
 * <p>Fails to "not enabled" while the game's settings are not stood up, which is the answer that
 * leaves every caller behaving as it does on an install without the mod - the composed sequence
 * this library writes itself.
 */
final class NexerelinPresence {

    private static final String MOD_ID = "nexerelin";

    private NexerelinPresence() {
    }

    static boolean isModEnabled() {

        var settings = Global.getSettings();

        if (settings == null || settings.getModManager() == null) {
            return false;
        }
        return settings.getModManager().isModEnabled(MOD_ID);
    }
}
