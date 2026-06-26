package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.lwjgl.util.vector.Vector2f;

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
}
