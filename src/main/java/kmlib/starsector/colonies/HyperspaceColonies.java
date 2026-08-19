package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.SectorAPI;

/**
 * Every owned colony sitting in hyperspace, selected by one rule, one entry per place and owner.
 *
 * <p>The hyperspace reader over {@link LocationColonies}, beside {@link Colonies}'s
 * star-system one. Vanilla puts no colony out here, but mods do, and hyperspace is not reachable
 * through {@code SectorAPI.getStarSystems()} - so without a reader that names it, a sector-wide
 * question silently answers as though those colonies did not exist.
 *
 * <p>Named separately rather than folded into the system reader because the two are different
 * questions with different answers, and a caller wanting one must not be handed the other. What
 * wants both asks {@link SectorColonies}, which says so.
 *
 * <p>Takes the sector rather than the location, hyperspace being the sector's own and there
 * being exactly one. A caller that had to fetch it first could pass a different location to a
 * method whose whole meaning is that it reads this one.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 */
public final class HyperspaceColonies {

    private HyperspaceColonies() {
        // utility class, no instances.
    }

    /**
     * Reads the colonies in the sector's hyperspace.
     *
     * <p>Selected by exactly the rule {@link LocationColonies#readColoniesIn} states, so a
     * colony out here counts on the same terms as one in a system - including the off-economy
     * ones, which is the shape a mod is most likely to have built out here in the first place.
     *
     * @param sector the sector whose hyperspace is read; null - or one with no hyperspace, which
     *               is what a sector reads as before it is built - yields an empty set
     * @return hyperspace's colonies, economy-listed ones first in economy order
     */
    public static Colonies readColonies(SectorAPI sector) {

        if (sector == null) {
            return Colonies.NONE;
        }
        return LocationColonies.readColoniesIn(sector, sector.getHyperspace());
    }
}
