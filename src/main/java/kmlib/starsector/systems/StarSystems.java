package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.starsector.markets.Markets;
import kmlib.starsector.rat.RandomAssortmentOfThingsMatcher;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Queries over a sector's star systems.
 *
 * <p>Centralises the common "walk {@code getStarSystems()} and pull something
 * off each" loops so KM* mods (and any external caller) share one
 * implementation rather than re-walking the system list each time.
 *
 * <p>Final class with a private constructor: pure-function utility, no
 * instance state. Matches {@link kmlib.starsector.scripts.SectorScripts}'s
 * shape, and is null-sector defensive like the rest of the library.
 */
public final class StarSystems {

    // A hang guard for a malformed cyclic orbit chain, not a domain limit: a real chain nests
    // only a few links (station -> planet -> star), so the walk always halts on the reference or
    // a null focus long before this. The value is generous headroom above any real nesting,
    // chosen only to bound a pathological cycle rather than to model one.
    private static final int MAX_ORBIT_CHAIN_DEPTH = 32;

    private StarSystems() {
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
        for (var system : sector.getStarSystems()) {
            var location = system.getLocation();
            if (location != null) {
                positions.add(new double[] {location.x, location.y});
            }
        }
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
    public static Map<String, double[]> collectPositionsById(SectorAPI sector,
            Predicate<StarSystemAPI> shouldInclude) {
        var positions = new LinkedHashMap<String, double[]>();
        if (sector == null) {
            return positions;
        }
        for (var system : sector.getStarSystems()) {
            var location = system.getLocation();
            if (location == null) {
                continue;
            }
            // A null predicate means no scoping was asked for, so every located
            // system is kept rather than the walk failing on the missing filter.
            if (shouldInclude != null && !shouldInclude.test(system)) {
                continue;
            }
            positions.put(system.getId(), new double[] {location.x, location.y});
        }
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
     * Every star in {@code system}, in the system's planet order - the set a
     * caller must disambiguate between when "the center" alone is ambiguous.
     * A single-star system has one unambiguous center to orbit; binary and
     * trinary systems orbit a shared, invisible center rather than any one
     * star, so a caller wanting a concrete focus has to pick a star itself.
     *
     * @param system the star system to read; null yields an empty list
     * @return each {@link PlanetAPI} in {@code system} for which
     *         {@link PlanetAPI#isStar()} holds, empty when none
     */
    public static List<PlanetAPI> getStars(StarSystemAPI system) {
        var stars = new ArrayList<PlanetAPI>();
        if (system == null) {
            return stars;
        }
        for (var planet : system.getPlanets()) {
            if (planet.isStar()) {
                stars.add(planet);
            }
        }
        return stars;
    }

    /**
     * Whether the player knows of at least one owned colony in {@code system},
     * under the normal known-to-player filter - the faction-presence read a map or
     * territory rule uses to admit a system as inhabited.
     *
     * @param sector the sector whose economy is read; null (or a null economy)
     *               yields false
     * @param system the system to test; null yields false
     * @return true when a known faction colony exists in the system
     */
    public static boolean hasKnownOwnedMarket(SectorAPI sector, StarSystemAPI system) {
        return hasKnownOwnedMarket(sector, system, false);
    }

    /**
     * Whether at least one owned colony exists in {@code system}. Composes the
     * ownership filter {@link Markets#isOwnedColony} with the visibility filter
     * {@link Markets#isKnownToPlayer}, so "counts as a known colony" means one thing
     * across every caller. Short-circuits on the first qualifying market.
     *
     * @param sector                           the sector whose economy is read; null
     *                                         (or a null economy) yields false
     * @param system                           the system to test; null yields false
     * @param shouldIncludeUndiscoveredMarkets whether an undiscovered colony still
     *                                         counts (the "show all factions" dev
     *                                         reveal); false applies the normal
     *                                         known-to-player filter, true drops it so
     *                                         an unfound colony counts too
     * @return true when a qualifying faction colony exists in the system
     */
    public static boolean hasKnownOwnedMarket(SectorAPI sector, StarSystemAPI system,
            boolean shouldIncludeUndiscoveredMarkets) {
        if (sector == null || system == null || sector.getEconomy() == null) {
            return false;
        }
        for (var market : sector.getEconomy().getMarkets(system)) {
            if (Markets.isCountedAsColony(market, shouldIncludeUndiscoveredMarkets)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How far a body sits from a reference along its orbit, summing the circular-orbit radii up
     * the body's orbit-focus chain until the chain reaches the reference - typically the
     * system's {@link #getCentralStar central star}.
     *
     * <p>Reads the orbit rather than the body's live position, so the value does not drift as
     * the body revolves: a planet is as far out as its orbit, a moon adds its planet's orbit, a
     * station on a planet adds the planet's too. A body that never reaches the reference sums
     * its whole chain - still a stable depth - and a malformed cyclic chain is capped rather
     * than looped forever. The reference's own orbit is not added, so a body sitting on the
     * reference reads zero.
     *
     * @param body      the body to measure; null yields {@link Double#POSITIVE_INFINITY}, since
     *                  a body with no orbit to read sits at no measurable distance
     * @param reference the body the chain is summed up to; null sums the whole chain to its root
     * @return the summed orbit-chain distance, or positive infinity when {@code body} is null
     */
    public static double getOrbitalDistanceTo(SectorEntityToken body, SectorEntityToken reference) {
        if (body == null) {
            return Double.POSITIVE_INFINITY;
        }
        var distance = 0.0;
        var orbiter = body;
        for (var depth = 0;
                orbiter != null && orbiter != reference && depth < MAX_ORBIT_CHAIN_DEPTH;
                depth++) {
            distance += orbiter.getCircularOrbitRadius();
            orbiter = orbiter.getOrbitFocus();
        }
        return distance;
    }

    /**
     * Decides whether a star system has a normal means of arrival.
     *
     * <p>Access is defined by the means of arrival a system actually offers, not
     * by trusting the {@code SYSTEM_CUT_OFF_FROM_HYPER} tag - that tag is set by
     * specific procgen paths, so a hand-built hidden system can be unreachable
     * without ever carrying it. An <em>active</em> gate always grants access.
     * Otherwise the system must be wired into hyperspace by at least one jump
     * point and not be flagged cut off. A transverse-only system - reachable
     * solely via a nascent gravity well, with no jump point - confers no access
     * and is not reachable, even though the engine never tags it cut off; a
     * present but inactive gate does not rescue it. Gate activation flips
     * {@link GateEntityPlugin#isActive}, so reachability tracks the real state.
     *
     * <p>Random Assortment of Things' Abyssal Fracture is a further means of
     * arrival: it ferries fleets in with a manual hyperspace transition rather
     * than a jump point, so a system entered only through a fracture carries no
     * jump point and would otherwise read as cut off. A fracture therefore grants
     * access the same way an active gate does, bypassing both the jump-point and
     * cut-off checks. RAT is optional, so the detection is delegated to
     * {@link RandomAssortmentOfThingsMatcher}, which is inert when RAT is absent.
     *
     * @param system the system to test
     * @return true when the player has a normal means of reaching it
     */
    public static boolean isReachable(StarSystemAPI system) {
        // A lit gate or an Abyssal Fracture reaches the system regardless of
        // jump connectivity, so either overrides the cut-off flag and the
        // absence of jump points.
        if (hasActiveGate(system) || hasAbyssalFracture(system)) {
            return true;
        }
        // No gate: the system must be reachable by ordinary hyperspace travel.
        // The cut-off flag rejects a system whose jump points are disabled; an
        // empty jump-point list rejects a transverse-only system whose sole
        // entry is a nascent gravity well (not a jump point).
        if (system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) {
            return false;
        }
        return !system.getJumpPoints().isEmpty();
    }

    /**
     * The first entity in {@code system} carrying {@code entityTag} whose id
     * equals {@code id} - the targeted "find this one tagged entity" lookup
     * console commands and scripts need (e.g. a specific gate or comm relay),
     * so callers do not re-walk {@code getEntitiesWithTag} themselves.
     *
     * @param system the star system to search; null yields null
     * @param entityTag the entity tag to filter on (e.g. {@code Tags.GATE})
     * @param id the entity id to match exactly; null or blank yields null
     * @return the first matching entity, or null when none in {@code system}
     *         carries {@code entityTag} with that id
     */
    public static SectorEntityToken find(StarSystemAPI system, String entityTag, String id) {
        if (system == null || !KmlibStrings.hasText(id)) {
            return null;
        }
        for (var entity : system.getEntitiesWithTag(entityTag)) {
            if (id.equals(entity.getId())) {
                return entity;
            }
        }
        return null;
    }

    // Whether any gate in the system is lit. An inactive gate (unscanned, or the
    // network not yet activated) grants nothing.
    private static boolean hasActiveGate(StarSystemAPI system) {
        for (var gate : system.getEntitiesWithTag(Tags.GATE)) {
            if (GateEntityPlugin.isActive(gate)) {
                return true;
            }
        }
        return false;
    }

    // Whether the system holds a RAT Abyssal Fracture. The matcher gates itself
    // on RAT being enabled, so this scans for nothing on a RAT-free install.
    private static boolean hasAbyssalFracture(StarSystemAPI system) {
        for (var entity : system.getAllEntities()) {
            if (RandomAssortmentOfThingsMatcher.isAbyssalFracture(entity)) {
                return true;
            }
        }
        return false;
    }
}
