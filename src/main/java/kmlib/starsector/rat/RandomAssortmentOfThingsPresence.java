package kmlib.starsector.rat;

import com.fs.starfarer.api.Global;

/**
 * Whether Random Assortment of Things is enabled this run - the gate every read of that mod
 * stands behind, and the one place its mod id is written.
 *
 * <p>Shared rather than repeated because the two reads in this package answer it for different
 * reasons and would drift apart: one defers loading a class that names a RAT type, the other keeps
 * a settings read that logs loudly on an unknown mod id from being made at all. A mod that renames
 * its id, or a guard that turns out to be needed, is then one edit rather than one per reader.
 *
 * <p>Fails to "not enabled" while the game's settings are not stood up, which is the answer that
 * leaves every caller behaving as it does on an install without the mod.
 */
final class RandomAssortmentOfThingsPresence {

    static final String MOD_ID = "assortment_of_things";

    private RandomAssortmentOfThingsPresence() {
    }

    static boolean isModEnabled() {

        var settings = Global.getSettings();

        if (settings == null || settings.getModManager() == null) {
            return false;
        }
        return settings.getModManager().isModEnabled(MOD_ID);
    }
}
