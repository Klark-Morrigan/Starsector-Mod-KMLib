package kmlib.starsector;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import java.util.function.Consumer;

/**
 * The one traversal of the sector's system list that every sector-wide read is built on.
 *
 * <p>The reads differ only in what each collects off a system, so the loop, the guard against a
 * sector that lists no systems, and the walk it charges live here once. Written out per read they
 * were as many places the counter could drift from what was visited - and as many places one
 * read could fail on a sector mid-teardown while its neighbours survived it.
 *
 * <p>Named at the root of the sector package rather than beside the system reads because the
 * market reads walk the sector too, and neither of those packages may depend on the other.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 */
public final class SectorWalks {

    private SectorWalks() {
        // utility class, no instances.
    }

    /**
     * Visits every star system the sector lists, in the sector's own order, and charges the walk
     * through {@link SectorWalkCounters#countSectorWalk}.
     *
     * <p>Charged at what the walk reached rather than at what a caller kept: a read keeping one
     * system of two did not make the traversal any shorter.
     *
     * @param sector      the sector to walk; null, or one listing no systems at all, visits nothing
     *                    and charges nothing, there having been no list to go over
     * @param visitSystem what to do with each system
     */
    public static void walkStarSystems(SectorAPI sector, Consumer<StarSystemAPI> visitSystem) {

        if (sector == null) {
            return;
        }

        var systems = sector.getStarSystems();
        if (systems == null) {
            return;
        }

        for (var system : systems) {
            visitSystem.accept(system);
        }

        SectorWalkCounters.countSectorWalk(systems.size());
    }
}
