package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.markets.Markets;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.List;

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
            if (Markets.isOwnedColony(market)
                    && (shouldIncludeUndiscoveredMarkets || Markets.isKnownToPlayer(market))) {
                return true;
            }
        }
        return false;
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
}
