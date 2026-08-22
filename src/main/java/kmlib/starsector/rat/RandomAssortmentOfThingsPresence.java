package kmlib.starsector.rat;

import kmlib.starsector.settings.ModPresence;

/**
 * Whether Random Assortment of Things is enabled this runs.
 */
public final class RandomAssortmentOfThingsPresence {

    public static final String MOD_ID = "assortment_of_things";

    private RandomAssortmentOfThingsPresence() { // utility class, no instances.
    }

    /**
     * @return whether Random Assortment of Things is enabled this run; false before the game has
     *         stood its mod set up, which is {@link ModPresence}'s answer rather than this one's
     */
    public static boolean isModEnabled() {
        return ModPresence.isModEnabled(MOD_ID);
    }
}
