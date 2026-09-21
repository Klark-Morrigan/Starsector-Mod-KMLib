package kmlib.mods.rat;

import kmlib.starsector.settings.modmanager.ModPresence;

/**
 * Whether Random Assortment of Things is enabled this runs.
 */
public final class RandomAssortmentOfThingsPresence {

    public static final String MOD_ID = "assortment_of_things";

    /**
     * The mod's name as a reader knows it: what a line about which mod supplies the way into a
     * system names, and what a report about this integration having stopped holding is headed with.
     *
     * <p>Beside the ID rather than at each of the places that show it, so a record latched under the
     * ID and a report naming the mod cannot drift into two mods. It matters more here than for most:
     * this mod's ID reads nothing like its name, so a heading falling back to the ID would name a
     * mod the player cannot find in their list.
     */
    public static final String MOD_NAME = "Random Assortment of Things";

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
