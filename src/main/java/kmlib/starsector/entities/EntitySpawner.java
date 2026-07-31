package kmlib.starsector.entities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

/**
 * Spawns sector entities, so KM* mods (and console tools) share one
 * implementation of "add a thing orbiting another thing" rather than repeating
 * the create-then-orbit dance. Creating the entity lives here; the orbit it is
 * placed on is delegated to {@link EntityOrbits}.
 */
public final class EntitySpawner {

    private EntitySpawner() {
    }

    /**
     * Adds a custom entity to {@code focus}'s location, orbiting {@code focus}.
     *
     * @param focus            the entity to orbit
     * @param entityType       the custom entity type id to spawn (e.g. a gate)
     * @param factionId        the owning faction id
     * @param orbitDistance    orbit radius from the focus
     * @param speedDegPerDay   orbital angular speed; 0 or less means static
     * @param startAngleDegrees angle from the focus at which the entity starts
     * @return the spawned entity
     */
    public static SectorEntityToken spawnOrbitingCustomEntity(
            SectorEntityToken focus,
            String entityType,
            String factionId,
            float orbitDistance,
            float speedDegPerDay,
            float startAngleDegrees) {

        var location = focus.getContainingLocation();
        // id and name left null: the engine auto-assigns a unique id and the
        // entity uses its type's default name. entityType is the type slot.
        var entity = location.addCustomEntity(null, null, entityType, factionId);
        EntityOrbits.applyCircularOrbit(
            entity,
            focus,
            orbitDistance,
            speedDegPerDay,
            startAngleDegrees);
        return entity;
    }

    /**
     * Creates a working wormhole jump point named {@code name} at {@code
     * focus}'s location, orbiting {@code focus}, with the standard
     * hyperspace-wormhole visual. Like {@link #spawnOrbitingCustomEntity} it derives
     * the location from the focus and adds the entity there.
     *
     * <p>The result is usable out of the box: a jump point is only useful once
     * it links to hyperspace and its system is reachable, so this also
     * generates the matching hyperspace-side entrance and clears any cut-off
     * tag on the containing star system (the engine's reachability signal). The
     * hyperspace wiring runs only when the focus is inside a star system; a
     * focus elsewhere just gets the placed-and-orbiting jump point.
     *
     * @param focus            the entity to orbit
     * @param name             the jump point's display name
     * @param orbitDistance    orbit radius from the focus
     * @param speedDegPerDay   orbital angular speed; 0 or less means static
     * @param startAngleDegrees angle from the focus at which the entity starts
     * @return the spawned jump point
     */
    public static JumpPointAPI spawnOrbitingJumpPoint(
            SectorEntityToken focus,
            String name,
            float orbitDistance,
            float speedDegPerDay,
            float startAngleDegrees) {

        var location = focus.getContainingLocation();
        var jumpPoint = Global.getFactory().createJumpPoint(null, name);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        location.addEntity(jumpPoint);
        
        EntityOrbits.applyCircularOrbit(
            jumpPoint,
            focus,
            orbitDistance,
            speedDegPerDay,
            startAngleDegrees);

        if (location instanceof StarSystemAPI) {
            var system = (StarSystemAPI) location;
            system.autogenerateHyperspaceJumpPoints();
            system.removeTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
            system.updateAllOrbits();
        }
        return jumpPoint;
    }
}
