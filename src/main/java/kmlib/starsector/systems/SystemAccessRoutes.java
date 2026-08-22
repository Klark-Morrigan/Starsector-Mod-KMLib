package kmlib.starsector.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which means of arrival this install has beyond the ones the game itself models, so that the
 * reachability read never names a mod. A mod with such a route registers here at load, and this
 * package never learns its name.
 *
 * <p>A collection rather than one slot, which is what separates this from
 * {@link kmlib.extensions.ExtensionPoint}. A point there holds work that is taken over whole, so a
 * second implementation behind the first would be one nothing ever reaches. Access is not taken
 * over: two mods can each add a way in, both are true at once, and keeping only the last registered
 * would silently drop one mod's routes because another loaded after it.
 *
 * <p>Keyed by the name registration was made under, so an integration composing itself twice
 * replaces its own entry rather than stacking a duplicate beside it. Insertion ordered, so routes
 * are consulted, and reported, in the order the install composed them.
 *
 * <p>Final class with a private constructor: the routes are the state, and they are one set per
 * running game rather than one per holder of a reference to it.
 */
public final class SystemAccessRoutes {

    private static final Logger LOG = Global.getLogger(SystemAccessRoutes.class);

    private static final Map<String, SystemAccessRoute> INSTALLED_ROUTES = new LinkedHashMap<>();

    private SystemAccessRoutes() {
        // utility class, no instances.
    }

    /**
     * Empties the set, so an install can be composed again from nothing rather than on top of what
     * a previous composition left.
     */
    public static void clearRoutes() {

        if (!INSTALLED_ROUTES.isEmpty()) {
            LOG.debug("Clearing system access routes: " + readRouteNames());
        }
        INSTALLED_ROUTES.clear();
    }

    /**
     * @return the names the installed routes were registered under, in the order they were
     *         registered; empty on every install running no mod that supplies one
     */
    public static List<String> readRouteNames() {
        return new ArrayList<>(INSTALLED_ROUTES.keySet());
    }

    /**
     * Adds a means of arrival to the ones this install already has.
     *
     * @param integrationName who reaches systems this way, for the log - a mod's name reads as the
     *                        answer to "what makes that system reachable on my install". A second
     *                        registration under a name already present replaces that one, an
     *                        integration composing itself twice being one route rather than two
     * @param accessRoute     the route to consult; null is passed over, an absent integration being
     *                        a state to leave alone rather than one that should disturb the routes
     *                        another mod did install
     */
    public static void registerRoute(String integrationName, SystemAccessRoute accessRoute) {

        if (accessRoute == null) {
            return;
        }

        // Said out loud for the same reason a displaced extension point is: a route that was meant
        // to be there and is not turns up much later as "that system reads as cut off", with
        // nothing anywhere naming the moment it was decided.
        if (INSTALLED_ROUTES.put(integrationName, accessRoute) != null) {
            LOG.info("System access route replaced under the same name: " + integrationName);
        } else {
            LOG.info("System access route installed: " + integrationName);
        }
    }

    // Whether any installed route reaches the system. Shaped as one question, so the read offering
    // it asks its own package once rather than walking a set it would then have to know the rules
    // of. An install with no routes answers false without touching a single entity.
    static boolean isReachedByAnyRoute(StarSystemAPI system) {

        for (var route : INSTALLED_ROUTES.values()) {
            if (route.isGrantingAccess(system)) {
                return true;
            }
        }
        return false;
    }
}
