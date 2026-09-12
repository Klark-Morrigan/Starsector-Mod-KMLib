package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.text.KmlibStrings;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Queries over the sector's whole set of star systems: where they sit, how to reach one by id or
 * by {@link SystemKey}, and which one the player is in.
 *
 * <p>Centralises the common "walk {@code getStarSystems()} and pull something off each" loops so
 * KM* mods (and any external caller) share one implementation rather than re-walking the system
 * list each time. What a caller then asks of a system it holds is {@link StarSystems}'.
 *
 * <p>Split from that class rather than kept beside it because the two are asked at different
 * moments: a pass resolves the sector's layout once and then asks about systems many times over.
 * Kept together they read as one grab-bag that any new system read could be added to.
 *
 * <p>Every read that traverses the sector's system list reports that through
 * {@link SectorWalkCounters}, so a caller's row states the walks it caused without the caller
 * having asked for any of them to be counted.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state. Matches
 * {@link kmlib.starsector.scripts.SectorScripts}'s shape, and is null-sector defensive like the
 * rest of the library.
 */
public final class SectorStarSystems {

    // Asked of log4j directly rather than of the game: the same logger under the same name, so
    // KMLib's level control still governs it, and every read here is handed the sector it is about
    // rather than reaching for the game's static entry point.
    private static final Logger LOG = Logger.getLogger(SectorStarSystems.class);

    private SectorStarSystems() {
        // utility class, no instances.
    }

    /**
     * Collects every star system's hyperspace position as an {x, y} point - the sector's spatial
     * layout, for callers that need it (e.g. partitioning the sector geometrically).
     *
     * @param sector the sector to read; null yields an empty list
     * @return one {x, y} pair per star system with a non-null location, in the sector's
     *         star-system order
     */
    public static List<double[]> collectHyperspacePositions(SectorAPI sector) {

        var positions = new ArrayList<double[]>();

        walkStarSystems(sector, system -> {
            var point = readPointOf(system);
            if (point != null) {
                positions.add(point);
            }
        });

        return positions;
    }

    /**
     * Collects the live hyperspace position of every star system the predicate selects, keyed by
     * system id - the id-addressed counterpart of {@link #collectHyperspacePositions}, for callers
     * that must match a position back to the system it belongs to (tracking motion, diffing a
     * layout).
     *
     * @param sector        the sector to read; null yields an empty map
     * @param shouldInclude which systems to keep; a system it rejects is left out. Null applies no
     *                      filter - every located system is kept, making this the id-keyed twin of
     *                      {@link #collectHyperspacePositions}
     * @return each selected system's {x, y} position keyed by id, in the sector's star-system
     *         order; a system with no location is skipped, having no position to record. An id is
     *         not unique (see {@link #indexByKey}), so several systems sharing one leave a single
     *         entry holding the last of their positions
     */
    public static Map<String, double[]> collectPositionsById(
            SectorAPI sector,
            Predicate<StarSystemAPI> shouldInclude) {

        var positions = new LinkedHashMap<String, double[]>();

        walkStarSystems(sector, system -> {
            var point = readPointOf(system);
            if (point == null) {
                return;
            }
            // A null predicate means no scoping was asked for, so every located
            // system is kept rather than the walk failing on the missing filter.
            if (shouldInclude != null && !shouldInclude.test(system)) {
                return;
            }
            positions.put(system.getId(), point);
        });

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
     * The star systems reachable by id, keyed by their {@code getId}, for a caller resolving many
     * ids rather than one.
     *
     * <p>The bulk counterpart of {@link #findById}, which walks the system list per lookup: a pass
     * resolving an id for each of a few hundred systems would otherwise walk that list once per
     * system. Keyed on {@code getId} for the same reason {@link #findById} matches on it - vanilla's
     * own {@code SectorAPI#getStarSystem} matches the optional unique id first and silently misses a
     * system whose base name is what every system-keyed map is built on.
     *
     * <p>Not every system in the sector: an id is not unique, and a modded install holds several
     * that share one. The first system carrying an id is the one indexed under it, which is the
     * system {@link #findById} answers with as well, so both id reads name the same system. Each
     * further one is stated in the log rather than quietly taking the entry. A pass that must reach
     * every system asks {@link #indexByKey} instead.
     *
     * @param sector the sector to index; null yields an empty map
     * @return the first system found under each id, in the sector's star-system order
     */
    public static Map<String, StarSystemAPI> indexById(SectorAPI sector) {

        var systemById = new LinkedHashMap<String, StarSystemAPI>();

        walkStarSystems(sector, system -> indexFirstSystemUnderId(systemById, system));

        return systemById;
    }

    /**
     * Every star system in the sector keyed by its {@link SystemKey} - the whole set, for a pass
     * that must account for each system rather than for each id.
     *
     * <p>What separates it from {@link #indexById}: a system id is not unique and a key is, so an
     * install whose sector holds several systems under one id holds an entry here for each of them.
     * A pass built on the id index instead is short those systems in everything it derives - their
     * cells, their positions, their motion - and the loss reads as a missing system on the map with
     * nothing naming its cause.
     *
     * @param sector the sector to index; null yields an empty map
     * @return each system keyed by its key, in the sector's star-system order
     */
    public static Map<SystemKey, StarSystemAPI> indexByKey(SectorAPI sector) {

        var systemByKey = new LinkedHashMap<SystemKey, StarSystemAPI>();

        walkStarSystems(sector, system -> systemByKey.put(SystemKey.readKeyOf(system), system));

        return systemByKey;
    }

    /**
     * The first star system whose {@code getId} equals {@code id} - the reliable id lookup
     * vanilla's own {@code SectorAPI#getStarSystem} does not provide. That one matches the optional
     * unique id before the base name, so a system keyed by its base name (which is what
     * {@code getId} returns) is silently missed whenever it also carries a unique id. This matches
     * {@code getId} directly, the id every system-keyed map is built on.
     *
     * <p>The first, because an id is not unique: a modded sector holds several systems sharing one.
     * That is the same system {@link #indexById} holds under the id, so an id resolves to one
     * system whichever way a caller asks.
     *
     * @param sector the sector to search; null yields null
     * @param id     the system id to match, as {@code StarSystemAPI#getId} reports it; null or
     *               blank yields null
     * @return the first system with that id, or null when none matches
     */
    public static StarSystemAPI findSystemById(SectorAPI sector, String id) {
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

    // The one traversal every bulk read here is built on: the reads differ only in what each
    // collects off a system, so the loop and the walk it charges live in one place. Written out per
    // read they were also as many places the counter could drift away from what was visited.
    //
    // A null sector charges nothing, there having been no list to go over.
    private static void walkStarSystems(SectorAPI sector, Consumer<StarSystemAPI> collectFrom) {

        if (sector == null) {
            return;
        }
        var systems = sector.getStarSystems();

        for (var system : systems) {
            collectFrom.accept(system);
        }
        // Charged at what the walk reached rather than at what a caller's filter kept: a read
        // keeping one system of two did not make the traversal any shorter.
        SectorWalkCounters.countSectorWalk(systems.size());
    }

    // Where a system sits, as the {x, y} pair every layout read here answers in, or nothing for a
    // system the sector never placed. Shared because the two position reads differ in what they key
    // the point by rather than in what a point is.
    private static double[] readPointOf(StarSystemAPI system) {

        var location = system.getLocation();

        return location == null ? null : new double[] {location.x, location.y};
    }

    // Puts a system under its id, keeping the one already indexed there.
    //
    // First rather than last so that this index and findSystemById, which stops at the first match,
    // name the same system for an id both hold - a preference or an override resolved through one
    // path otherwise addresses a different system than the same id resolved through the other.
    //
    // Said out loud because the alternative is what the silent displacement cost: a pass built on
    // this index is short a system, and nothing connects that to two systems sharing an id.
    private static void indexFirstSystemUnderId(
            Map<String, StarSystemAPI> systemById,
            StarSystemAPI system) {

        var indexed = systemById.putIfAbsent(system.getId(), system);
        if (indexed == null) {
            return;
        }
        LOG.warn("Star system id is not unique: '" + system.getId()
            + "' is held by '" + StarSystems.readDisplayName(indexed)
            + "', so '" + StarSystems.readDisplayName(system)
            + "' is absent from the id index");
    }
}
