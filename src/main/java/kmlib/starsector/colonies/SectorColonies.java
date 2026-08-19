package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.SectorAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Every owned colony in the sector, one entry per place and owner.
 *
 * <p>Where {@link Colonies} answers "who is here" for one star system and
 * {@link HyperspaceColonies} for hyperspace, this is the composition of the two - the read
 * behind any question posed of the sector rather than of one place: what a faction holds in
 * total, where a given owner is present, how much of the sector is settled at all.
 *
 * <p>It composes those two readers and does nothing else. Every rule about what counts - a
 * colony's ownership, one entry per place when several markets share a body, whether a colony
 * the economy never registered is present - belongs to {@link LocationColonies} beneath them
 * and is inherited here. Walking the economy directly instead would be a second answer to all
 * three, free to disagree with the surfaces that paint the sector from the colony sets.
 *
 * <p>Hyperspace is why this exists as a named read rather than a loop at each call site.
 * {@code SectorAPI.getStarSystems()} does not reach it, so a caller writing the loop itself
 * omits any colony out there without ever noticing - vanilla builds none, but mods put markets
 * in hyperspace and a sector-wide count that quietly drops them is wrong rather than merely
 * incomplete.
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
     * <p>A sector listing no systems is still asked for its hyperspace. The two are separate
     * places, so one being unreadable says nothing about the other - and treating a missing
     * system list as "the sector holds nothing" would drop exactly the colonies this read exists
     * to catch.
     *
     * <p>Unfogged, as the colony sets are. Whether the player may be shown a colony is a
     * question about a reader's purpose rather than about the sector, so it is left to the
     * reader - a dev listing counts what has not been found, while a map must not paint it.
     *
     * @param sector the sector to walk; null yields an empty list
     * @return every owned colony in the sector; never null
     */
    public static List<Colony> readColonies(SectorAPI sector) {

        if (sector == null) {
            return List.of();
        }
        var colonies = new ArrayList<Colony>();
        var systems = sector.getStarSystems();

        if (systems != null) {
            for (var system : systems) {
                colonies.addAll(SystemColonies.readColoniesIn(sector, system).colonies());
            }
        }
        colonies.addAll(HyperspaceColonies.readColonies(sector).colonies());

        return colonies;
    }
}
