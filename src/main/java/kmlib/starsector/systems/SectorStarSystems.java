package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.text.KmlibStrings;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Queries over the sector's whole set of star systems: where they sit, how to reach one by ID or
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
            // A system the sector never placed has no point to record.
            var location = system.getLocation();
            if (location != null) {
                positions.add(new double[] {location.x, location.y});
            }
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
     * The star systems reachable by ID, keyed by their {@code getId}, for a caller resolving many
     * IDs rather than one.
     *
     * <p>The bulk counterpart of {@link #findSystemById}, which walks the system list per lookup: a
     * pass resolving an ID for each of a few hundred systems would otherwise walk that list once per
     * system. Keyed on {@code getId} for the same reason {@link #findSystemById} matches on it -
     * vanilla's own {@code SectorAPI#getStarSystem} matches the optional unique ID first and
     * silently misses a system whose base name is what every system-keyed map is built on.
     *
     * <p>Not every system in the sector: an ID is not unique, and a modded install holds several
     * that share one. The first system carrying an ID is the one indexed under it, which is the
     * system {@link #findSystemById} answers with as well, so both ID reads name the same system.
     * Each further one is stated in the log rather than quietly taking the entry. A pass that must
     * reach every system asks {@link #indexByKey} instead.
     *
     * <p>Read off the key index rather than by a walk of its own, so the sector is traversed once
     * however a caller asks for it. The complete index is the one a coarser address is taken from,
     * and never the other way round: an index that had already dropped a system could not be asked
     * for it again.
     *
     * @param sector the sector to index; null yields an empty map
     * @return the first system found under each ID, in the sector's star-system order
     */
    public static Map<String, StarSystemAPI> indexById(SectorAPI sector) {
        return indexHeldSystemsById(indexByKey(sector).values());
    }

    /**
     * The same ID index over systems a caller already holds - for one that has traversed the sector
     * already and would otherwise traverse it again to address those systems a second way.
     *
     * <p>Answers on {@link #indexById}'s terms, differing only in where the systems come from: the
     * first system carrying an ID is the one indexed under it, and each further one is stated in
     * the log. Charges no walk, the traversal having been the caller's.
     *
     * @param systems the systems to index, in the order they are to be indexed in; null yields an
     *                empty map
     * @return the first system found under each ID, in the order given
     */
    public static Map<String, StarSystemAPI> indexHeldSystemsById(
            Collection<StarSystemAPI> systems) {

        var systemById = new LinkedHashMap<String, StarSystemAPI>();

        if (systems == null) {
            return systemById;
        }
        for (var system : systems) {
            indexFirstSystemUnderId(systemById, system);
        }
        return systemById;
    }

    /**
     * Every star system in the sector keyed by its {@link SystemKey} - the whole set, for a pass
     * that must account for each system rather than for each id.
     *
     * <p>What separates it from {@link #indexById}: a system ID is not unique and a key is, so an
     * install whose sector holds several systems under one ID holds an entry here for each of them.
     * A pass built on the ID index instead is short those systems in everything it derives - their
     * cells, their positions, their motion - and the loss reads as a missing system on the map with
     * nothing naming its cause.
     *
     * <p>The complete index, and so the one every coarser address is taken from. Two systems meet
     * in one entry only where the sector states the same three arms about both, which it does for
     * the system it states nothing about at all; the first is kept and the other is stated in the
     * log.
     *
     * @param sector the sector to index; null yields an empty map
     * @return the first system found under each key, in the sector's star-system order
     */
    public static Map<SystemKey, StarSystemAPI> indexByKey(SectorAPI sector) {

        var systemByKey = new LinkedHashMap<SystemKey, StarSystemAPI>();

        walkStarSystems(sector, system -> indexFirstSystemUnderKey(systemByKey, system));

        return systemByKey;
    }

    /**
     * The first star system whose {@code getId} equals {@code id} - the reliable ID lookup
     * vanilla's own {@code SectorAPI#getStarSystem} does not provide. That one matches the optional
     * unique ID before the base name, so a system keyed by its base name (which is what
     * {@code getId} returns) is silently missed whenever it also carries a unique id. This matches
     * {@code getId} directly, the ID every system-keyed map is built on.
     *
     * <p>The first, because an ID is not unique: a modded sector holds several systems sharing one.
     * That is the same system {@link #indexById} holds under the ID, so an ID resolves to one
     * system whichever way a caller asks.
     *
     * @param sector the sector to search; null yields null
     * @param id     the system ID to match, as {@code StarSystemAPI#getId} reports it; null or
     *               blank yields null
     * @return the first system with that ID, or null when none matches
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

    /**
     * The star system whose {@link SystemKey} equals {@code key} - the lookup for a caller holding
     * a key rather than an id.
     *
     * <p>The same walk {@link #findSystemById} makes, comparing all three arms instead of the ID
     * alone, so it names one system where the ID lookup names the first of however many share an
     * id. A key stating nothing at all matches nothing: it equals every other blank key, so the
     * system it answered with would be whichever the sector listed first.
     *
     * @param sector the sector to search; null yields null
     * @param key    the key to match, as {@link SystemKey#readKeyOf} reads it; null or stating no
     *               arm yields null
     * @return the system carrying that key, or null when none matches
     */
    public static StarSystemAPI findSystemByKey(SectorAPI sector, SystemKey key) {
        if (sector == null || key == null || !key.hasStatedArm()) {
            return null;
        }
        StarSystemAPI found = null;
        var systemsExamined = 0;

        for (var system : sector.getStarSystems()) {
            systemsExamined++;
            if (key.equals(SystemKey.readKeyOf(system))) {
                found = system;
                break;
            }
        }
        // Reported on the one exit for the reason the ID lookup reports on its own: a match part way
        // through still charges what it went over rather than the whole sector.
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

    // Puts a system under its ID, keeping the one already indexed there.
    //
    // First rather than last so that this index and findSystemById, which stops at the first match,
    // name the same system for an ID both hold - a preference or an override resolved through one
    // path otherwise addresses a different system than the same ID resolved through the other.
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

    // Puts a system under its key, keeping the one already indexed there.
    //
    // Two systems meet here only where the sector states the same ID and the same two entities about
    // both - in practice where it states none of the three - so nothing is left to tell them apart
    // and the choice between them is arbitrary. Kept first for the same reason every other read here
    // keeps the first, so that one system is named however a caller addresses it.
    //
    // Said out loud because this is the index promised to hold every system the sector lists: one
    // lost here is lost from every structure derived from it, and nothing else connects that to a
    // sector stating two systems identically.
    private static void indexFirstSystemUnderKey(
            Map<SystemKey, StarSystemAPI> systemByKey,
            StarSystemAPI system) {

        var indexed = systemByKey.putIfAbsent(SystemKey.readKeyOf(system), system);
        if (indexed == null) {
            return;
        }
        LOG.warn("Star system key is not unique: '"
            + StarSystems.readDisplayName(indexed)
            + "' and '" + StarSystems.readDisplayName(system)
            + "' state the same id, centre and anchor, so the second is absent from the key index");
    }
}
