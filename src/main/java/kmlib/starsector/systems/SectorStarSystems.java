package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Queries over the sector's whole set of star systems: where they sit, how to reach one by id,
 * and which one the player is in.
 *
 * <p>Centralises the common "walk {@code getStarSystems()} and pull something off each" loops so
 * KM* mods (and any external caller) share one implementation rather than re-walking the system
 * list each time. What a caller then asks of a system it holds is {@link StarSystems}'.
 *
 * <p>Split from that class rather than kept beside it because the two are asked at different
 * moments: a pass resolves the sector's layout once and then asks about systems many times over.
 * Kept together they read as one grab-bag that any new system read could be added to.
 *
 * <p>Every read here traverses the sector's system list, and each one reports that through
 * {@link SectorWalkCounters}, so a caller's row states the walks it caused without the caller
 * having asked for any of them to be counted.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state. Matches
 * {@link kmlib.starsector.scripts.SectorScripts}'s shape, and is null-sector defensive like the
 * rest of the library.
 */
public final class SectorStarSystems {

    private SectorStarSystems() {
        // utility class, no instances.
    }

    /**
     * Collects every star system's hyperspace position as an {x, y} point -
     * the sector's spatial layout, for callers that need it (e.g.
     * partitioning the sector geometrically).
     *
     * @param sector the sector to read; null yields an empty list.
     * @return one {x, y} pair per star system with a non-null location, in
     *         the sector's star-system order.
     */
    public static List<double[]> getHyperspacePositions(SectorAPI sector) {

        var positions = new ArrayList<double[]>();
        if (sector == null) {
            return positions;
        }
        var systems = sector.getStarSystems();

        for (var system : systems) {
            var location = system.getLocation();
            if (location != null) {
                positions.add(new double[] {location.x, location.y});
            }
        }
        SectorWalkCounters.countSectorWalk(systems.size());

        return positions;
    }

    /**
     * Collects the live hyperspace position of every star system the predicate
     * selects, keyed by system id - the id-addressed counterpart of
     * {@link #getHyperspacePositions}, for callers that must match a position back to
     * the system it belongs to (tracking motion, diffing a layout).
     *
     * @param sector        the sector to read; null yields an empty map
     * @param shouldInclude which systems to keep; a system it rejects is left out.
     *                      Null applies no filter - every located system is kept,
     *                      making this the id-keyed twin of
     *                      {@link #getHyperspacePositions}
     * @return each selected system's {x, y} position keyed by id, in the sector's
     *         star-system order; a system with no location is skipped, having no
     *         position to record
     */
    public static Map<String, double[]> collectPositionsById(
            SectorAPI sector,
            Predicate<StarSystemAPI> shouldInclude) {

        var positions = new LinkedHashMap<String, double[]>();
        if (sector == null) {
            return positions;
        }
        var systems = sector.getStarSystems();

        for (var system : systems) {
            var location = system.getLocation();
            if (location == null) {
                continue;
            }
            // A null predicate means no scoping was asked for, so every located
            // system is kept rather than the walk failing on the missing filter.
            if (shouldInclude != null && !shouldInclude.test(system)) {
                continue;
            }
            positions.put(
                system.getId(),
                new double[] {location.x, location.y});
        }
        // The whole list was gone over whatever the predicate kept, so the walk is
        // charged at what it reached rather than at what survived the filter.
        SectorWalkCounters.countSectorWalk(systems.size());

        return positions;
    }

    /**
     * The star system the player's fleet is currently in, for callers (e.g.
     * in-system-only console commands) that must act on the current system.
     *
     * @param sector the sector to read; null yields null
     * @return the player's current star system, or null when the fleet is in
     *         hyperspace or unavailable
     */
    public static StarSystemAPI getPlayerStarSystem(SectorAPI sector) {
        if (sector == null || sector.getPlayerFleet() == null) {
            return null;
        }
        return sector.getPlayerFleet().getStarSystem();
    }

    /**
     * Every star system in the sector keyed by its {@code getId}, for a caller resolving many ids
     * rather than one.
     *
     * <p>The bulk counterpart of {@link #findById}, which walks the system list per lookup: a pass
     * resolving an id for each of a few hundred systems would otherwise walk that list once per
     * system. Keyed on {@code getId} for the same reason {@link #findById} matches on it - vanilla's
     * own {@code SectorAPI#getStarSystem} matches the optional unique id first and silently misses a
     * system whose base name is what every system-keyed map is built on.
     *
     * @param sector the sector to index; null yields an empty map
     * @return each system keyed by its id, in the sector's star-system order
     */
    public static Map<String, StarSystemAPI> indexById(SectorAPI sector) {

        var systemById = new LinkedHashMap<String, StarSystemAPI>();
        if (sector == null) {
            return systemById;
        }
        var systems = sector.getStarSystems();

        for (var system : systems) {
            systemById.put(system.getId(), system);
        }
        SectorWalkCounters.countSectorWalk(systems.size());

        return systemById;
    }

    /**
     * The star system whose {@code getId} equals {@code id} - the reliable id lookup vanilla's own
     * {@code SectorAPI#getStarSystem} does not provide. That one matches the optional unique id
     * before the base name, so a system keyed by its base name (which is what {@code getId}
     * returns) is silently missed whenever it also carries a unique id. This matches {@code getId}
     * directly, the id every system-keyed map is built on.
     *
     * @param sector the sector to search; null yields null
     * @param id     the system id to match, as {@code StarSystemAPI#getId} reports it; null or
     *               blank yields null
     * @return the system with that id, or null when none matches
     */
    public static StarSystemAPI findById(SectorAPI sector, String id) {
        if (sector == null || !KmlibStrings.hasText(id)) {
            return null;
        }
        StarSystemAPI found = null;
        var systemsExamined = 0;

        for (var system : sector.getStarSystems()) {
            systemsExamined++;
            if (id.equals(system.getId())) {
                found = system;
                break;
            }
        }
        // One exit, so a match part way through still reports its walk - and reports
        // it at what it went over, a lookup that stopped at the first system not
        // having visited the sector.
        SectorWalkCounters.countSectorWalk(systemsExamined);

        return found;
    }
}
