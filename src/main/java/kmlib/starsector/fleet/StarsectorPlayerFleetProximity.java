package kmlib.starsector.fleet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.starsector.geometry.StarsectorPoints;

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
 * <p>A {@code null} planet, sector or player fleet reads as
 * <strong>not</strong> in orbit. Callers that want a different
 * default (e.g. "fee stays on when sector state is incomplete") branch
 * on the returned boolean rather than asking the helper to invert its
 * defensive default.
 */
public final class StarsectorPlayerFleetProximity {

    private StarsectorPlayerFleetProximity() {
    }

    /**
     * Returns {@code true} when the player fleet of the sector {@code Global} holds sits within
     * {@code planet.getRadius() + orbitOffset} of the planet. Prefer
     * {@link #isPlayerFleetInOrbitOf(SectorAPI, PlanetAPI, float)}, which reads the sector the caller hands in.
     *
     * @param planet      the planet to measure from
     * @param orbitOffset the distance past the planet's radius that still counts as orbit
     * @return whether the player fleet is in orbit
     */
    public static boolean isPlayerFleetInOrbitOf(PlanetAPI planet, float orbitOffset) {

        return isPlayerFleetInOrbitOf(Global.getSector(), planet, orbitOffset);
    }

    /**
     * Returns {@code true} when {@code sector}'s player fleet sits within {@code planet.getRadius() + orbitOffset}
     * of the planet. A {@code null} planet, sector or player fleet returns {@code false} - see the class doc for the
     * defensive-default rationale.
     *
     * <p>The threshold is inclusive ({@code <=}) so a hairline-precise approach does not flip the classification
     * mid-frame when the fleet velocity happens to land it exactly on the band.
     *
     * @param sector      the sector whose player fleet is measured
     * @param planet      the planet to measure from
     * @param orbitOffset the distance past the planet's radius that still counts as orbit
     * @return whether the player fleet is in orbit
     */
    public static boolean isPlayerFleetInOrbitOf(SectorAPI sector, PlanetAPI planet, float orbitOffset) {

        if (planet == null || sector == null) {
            return false;
        }

        var playerFleet = sector.getPlayerFleet();
        if (playerFleet == null) {
            return false;
        }

        var distance = StarsectorPoints.computeDistanceBetween(playerFleet, planet);

        return distance <= planet.getRadius() + orbitOffset;
    }
}
