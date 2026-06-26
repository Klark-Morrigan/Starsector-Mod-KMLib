package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.lwjgl.util.vector.Vector2f;
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
