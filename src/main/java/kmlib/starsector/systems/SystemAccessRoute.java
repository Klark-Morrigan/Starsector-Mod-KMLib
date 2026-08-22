package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.StarSystemAPI;

/**
 * A means of arrival belonging to something other than this library - a way an installed mod
 * carries fleets into a system that the vanilla jump-point and cut-off reads cannot see.
 *
 * <p>It exists because whether a system can be reached is a question about hyperspace, while the
 * answer can turn on a single mod's entity. Random Assortment of Things moves fleets through an
 * Abyssal Fracture with a manual hyperspace transition rather than a jump point, so a system
 * entered only that way holds no jump point and may carry the cut-off tag while being perfectly
 * reachable. Stated as a route, that exception is supplied by whoever knows the mod, and the
 * reachability read stays a question about hyperspace with no mod named in it.
 *
 * <p>A route says only that access exists, never that it is the only one. Routes are consulted
 * alongside the vanilla reads and alongside each other, so one answering false leaves the question
 * exactly where it found it - which is what lets an install carry several without them having to
 * agree about anything.
 */
@FunctionalInterface
public interface SystemAccessRoute {

    /**
     * @param system the system being asked about; never null
     * @return whether this route reaches that system
     */
    boolean isGrantingAccess(StarSystemAPI system);
}
