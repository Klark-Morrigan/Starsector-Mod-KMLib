package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Every owned colony in the sector, one entry per place and owner.
 *
 * <p>Where {@link SystemColonies} answers "who is here" for one location, this answers it for
 * all of them at once - the read behind any question posed of the sector rather than of a
 * system: what a faction holds in total, where a given owner is present, how much of the sector
 * is settled at all.
 *
 * <p>It is a walk over {@link SystemColonies} and nothing more. Every rule about what counts -
 * a colony's ownership, one entry per place when several markets share a body, whether a colony
 * the economy never registered is present - lives there and is inherited here. Walking the
 * economy directly instead would be a second answer to all three, free to disagree with the
 * surfaces that paint the sector from the colony sets.
 *
 * <p>Hyperspace is walked alongside the star systems, and that is the whole reason this is not
 * simply a loop at each call site. {@code SectorAPI.getStarSystems()} does not reach it, so a
 * caller writing the loop itself omits any colony out there without ever noticing - vanilla
 * builds none, but mods put markets in hyperspace and a sector-wide count that quietly drops
 * them is wrong rather than merely incomplete.
 *
 * <p>Yields the colonies rather than any tally of them. What a caller wants counted differs per
 * caller - places, hidden places, owners, systems - and a count computed here would be one
 * caller's question answered on everyone's behalf.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 */
public final class SectorColonies {

    private SectorColonies() {
        // utility class, no instances.
    }

    /**
     * Reads every owned colony in the sector, star systems first in the sector's own order and
     * hyperspace last.
     *
     * <p>The order is the sector's rather than one imposed here, so two runs over one save
     * report the same thing in the same order. Hyperspace goes last because it is not a system:
     * a reader listing what it finds should reach the ordinary case first.
     *
     * <p>Unfogged, as the colony sets are. Whether the player may be shown a colony is a
     * question about a reader's purpose rather than about the sector, so it is left to the
     * reader - a dev listing counts what has not been found, while a map must not paint it.
     *
     * @param sector the sector to walk; null (or one with no star systems) yields an empty list
     * @return every owned colony in the sector; never null
     */
    public static List<SystemColony> readColonies(SectorAPI sector) {

        if (sector == null || sector.getStarSystems() == null) {
            return List.of();
        }
        var colonies = new ArrayList<SystemColony>();

        for (var system : sector.getStarSystems()) {
            colonies.addAll(SystemColonies.readColoniesIn(sector, system).colonies());
        }
        // Read through the same set as any system, hyperspace being just another location the
        // economy places markets in. A null hyperspace reads as empty rather than faulting,
        // matching how the colony set itself treats a location it cannot reach.
        colonies.addAll(SystemColonies.readColoniesIn(sector, sector.getHyperspace()).colonies());

        return colonies;
    }
}
