package kmlib.starsector.rat;

import kmlib.starsector.settings.ModPresence;

/**
 * Whether Random Assortment of Things is enabled this run - the gate every read of that mod
 * stands behind, and the one place its mod id is written.
 *
 * <p>Shared rather than repeated because the two reads in this package answer it for different
 * reasons and would drift apart: one defers loading a class that names a RAT type, the other keeps
 * a settings read that logs loudly on an unknown mod id from being made at all. A mod that renames
 * its id, or a guard that turns out to be needed, is then one edit rather than one per reader.
 *
 * <p>What is left here is the id and the name for it. How the mod set is asked, and what a read
 * taken before the game has stood one up answers, is {@link ModPresence}'s - the same manner of
 * asking every optional-mod gate in the library uses.
 */
public final class RandomAssortmentOfThingsPresence {

    static final String MOD_ID = "assortment_of_things";

    private RandomAssortmentOfThingsPresence() {
    }

    /**
     * @return whether Random Assortment of Things is enabled this run; false before the game has
     *         stood its mod set up, which is {@link ModPresence}'s answer rather than this one's
     */
    public static boolean isModEnabled() {
        return ModPresence.isModEnabled(MOD_ID);
    }
}
