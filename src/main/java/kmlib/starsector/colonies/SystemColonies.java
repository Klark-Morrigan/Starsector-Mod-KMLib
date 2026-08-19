package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

/**
 * Every owned colony in one star system, selected by one rule, one entry per place and owner.
 *
 * <p>The star-system reader over {@link LocationColonies}, beside
 * {@link HyperspaceColonies}'s. The selection rule is that one's and is documented there; what
 * this adds is that the place asked about is a star system and can be nothing else.
 *
 * <p>Named separately rather than folded into a single read taking any location, so a caller
 * says which kind of place it means and a hyperspace colony cannot arrive through a read that
 * says "system". What wants both asks {@link SectorColonies}.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 */
public final class SystemColonies {

    private SystemColonies() {
        // utility class, no instances.
    }

    /**
     * Reads {@code system}'s colonies out of the sector - the single walk every reader of that
     * system is meant to share rather than repeat.
     *
     * <p>Selected by exactly the rule {@link LocationColonies#readColoniesIn} states, so a colony
     * in a system counts on the same terms as one anywhere else.
     *
     * @param sector the sector whose economy and systems are read; null yields an empty set,
     *               there being no listing to select from
     * @param system the system to read; null yields an empty set
     * @return the system's colonies, economy-listed ones first in economy order
     */
    public static Colonies readColoniesIn(SectorAPI sector, StarSystemAPI system) {
        return LocationColonies.readColoniesIn(sector, system);
    }
}
