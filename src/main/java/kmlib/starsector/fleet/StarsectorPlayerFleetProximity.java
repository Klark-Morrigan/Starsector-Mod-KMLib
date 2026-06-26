package kmlib.starsector.fleet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;

import kmlib.math.geometry.Points;


/**
 * Player-fleet-to-planet proximity helper. Centralises the
 * {@code Misc.getDistance(playerFleet, planet) <= planet.radius +
 * extraOffset} idiom that vanilla, LazyLib, and most mods hand-roll at
 * every call site (see e.g.
 * {@code BattleCreationPluginImpl#getEntityToFightAt}).
 *
 * <p>The planet's radius is baked in because every realistic
 * "is the player in orbit?" check scales the band with planet size -
 * a small moon's orbit lane sits much closer than a gas giant's. The
 * caller supplies the additional offset (the part that varies by use
 * case: dialog trigger range, dropship band, scanner reach, ...), so
 * the helper stays a single-purpose primitive rather than growing
 * usage-specific knobs.
 *
 * <p>Both a {@code null} planet and a {@code null} player fleet read
 * as <strong>not</strong> in orbit. Callers that want a different
 * default (e.g. "fee stays on when sector state is incomplete") branch
 * on the returned boolean rather than asking the helper to invert its
 * defensive default.
 */
public final class StarsectorPlayerFleetProximity {

    private StarsectorPlayerFleetProximity() {
    }

    /**
     * Returns {@code true} when the player fleet sits within
     * {@code planet.getRadius() + orbitOffset} of the planet. A
     * {@code null} planet or a missing player fleet returns
     * {@code false} - see the class doc for the defensive-default
     * rationale.
     *
     * <p>The threshold is inclusive ({@code <=}) so a hairline-precise
     * approach does not flip the classification mid-frame when the
     * fleet velocity happens to land it exactly on the band.
     */
    public static boolean isPlayerFleetInOrbitOf(PlanetAPI planet, float orbitOffset) {
        if (planet == null) {
            return false;
        }
        var playerFleet = Global.getSector() == null
                ? null
                : Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            return false;
        }
        var fleetLocation = playerFleet.getLocation();
        var planetLocation = planet.getLocation();
        var distance = Points.computeDistance(fleetLocation.x, fleetLocation.y,
                planetLocation.x, planetLocation.y);
        return distance <= planet.getRadius() + orbitOffset;
    }
}
