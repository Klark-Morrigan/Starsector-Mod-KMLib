package kmlib.mods.rat;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.SectorWalkCounters;

import assortment_of_things.abyss.entities.hyper.AbyssalFracture;

/**
 * Recognises Random Assortment of Things' Abyssal Fracture so a caller can treat
 * it as a means of access even though it is not a jump point. RAT routes fleets
 * through the fracture with a manual hyperspace transition on its custom entity
 * plugin, bypassing the vanilla jump-point system, so connectivity checks that
 * inspect only jump points never see it.
 *
 * <p>RAT is an optional dependency. The match is gated behind a mod-enabled
 * check, and the sole reference to the RAT type lives in the nested
 * {@link RatTypes} holder, which the classloader does not resolve until the
 * gate has passed. That keeps an install without RAT from ever loading
 * {@link AbyssalFracture}, and so from failing with a missing-class error.
 */
public final class RandomAssortmentOfThingsMatcher {

    private RandomAssortmentOfThingsMatcher() {
    }

    /**
     * Whether the system holds an Abyssal Fracture anywhere in it.
     *
     * <p>Stated over the system rather than over an entity because that is the shape the
     * question is asked in: what matters is that a fracture is in there, not which entity it
     * is. The mod-enabled gate is then passed once per system rather than once per entity.
     *
     * @param system the system being asked about; null yields false
     * @return true when RAT is enabled and any entity in the system is an Abyssal Fracture
     */
    public static boolean hasAbyssalFracture(StarSystemAPI system) {
        if (system == null || !RandomAssortmentOfThingsPresence.isModEnabled()) {
            return false;
        }
        return RatTypes.hasAbyssalFracture(system);
    }

    // Isolates the only reference to a RAT type. The classloader resolves this
    // holder on first call, which the gate above defers until RAT is known to
    // be present, so AbyssalFracture is never sought otherwise.
    private static final class RatTypes {

        private static boolean hasAbyssalFracture(StarSystemAPI system) {

            var hasFracture = false;
            var entitiesExamined = 0;

            for (var entity : system.getAllEntities()) {
                entitiesExamined++;
                if (isAbyssalFracture(entity)) {
                    hasFracture = true;
                    break;
                }
            }
            // One exit, so a scan that stopped at the fracture still reports what it
            // went over: this runs per system inside a reachability read, and what it
            // costs is the entities examined rather than the answer.
            SectorWalkCounters.countEntitiesVisited(entitiesExamined);

            return hasFracture;
        }

        private static boolean isAbyssalFracture(SectorEntityToken entity) {
            return entity.getCustomPlugin() instanceof AbyssalFracture;
        }
    }
}
