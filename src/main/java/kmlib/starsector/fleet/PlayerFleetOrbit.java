package kmlib.starsector.fleet;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.starsector.geometry.StarsectorPoints;

/**
 * Whether the player fleet is in orbit of an entity: the {@code distance <= radius + offset} check vanilla and most
 * mods hand-roll at each call site, as {@code BattleCreationPluginImpl} does for corona shelters.
 *
 * <p>The entity's radius is part of the band because an orbit scales with the body - a small moon's lane sits much
 * closer than a gas giant's. The caller supplies only the offset past it, which is the part that varies between uses.
 *
 * <p>An absent fleet or entity answers {@code false}, because nothing absent is in orbit. A sector has no player fleet
 * while a new game is being generated.
 */
public final class PlayerFleetOrbit {

    private PlayerFleetOrbit() {
    }

    /**
     * Returns {@code true} when {@code sector}'s player fleet shares {@code entity}'s location and sits within
     * {@code entity.getRadius() + orbitOffset} of it. The band edge counts as in orbit.
     *
     * <p>The location check comes first because coordinates are local to a star system or hyperspace. Without it, a
     * fleet in another system at the same coordinates would read as in orbit.
     *
     * @param sector      the sector whose player fleet is measured
     * @param entity      the entity to measure from, possibly {@code null}
     * @param orbitOffset the distance past the entity's radius that still counts as orbit
     * @return whether the player fleet is in orbit
     */
    public static boolean isPlayerFleetInOrbitOf(SectorAPI sector, SectorEntityToken entity, float orbitOffset) {

        var playerFleet = sector.getPlayerFleet();
        if (playerFleet == null || entity == null) {
            return false;
        }

        if (playerFleet.getContainingLocation() != entity.getContainingLocation()) {
            return false;
        }

        var distance = StarsectorPoints.computeDistanceBetween(playerFleet, entity);

        return distance <= entity.getRadius() + orbitOffset;
    }
}
