package kmlib.starsector.rat;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

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
     * Whether the system holds an Abyssal Fracture anywhere in it - the whole-system read, which
     * is the shape reachability is asked in: what matters there is that a fracture is in the
     * system, not which entity it is.
     *
     * <p>The mod-enabled gate is passed once here rather than once per entity, so a system on a
     * RAT-free install is answered without walking its entities at all, and one on a RAT install
     * asks the mod set a single question however many entities it holds.
     *
     * @param system the system being asked about; null yields false
     * @return true when RAT is enabled and any entity in the system is an Abyssal Fracture
     */
    public static boolean hasAbyssalFracture(StarSystemAPI system) {
        // Short-circuits before touching RatTypes for the same reason the per-entity read does.
        if (system == null || !RandomAssortmentOfThingsPresence.isModEnabled()) {
            return false;
        }
        return RatTypes.hasAbyssalFracture(system);
    }

    /**
     * @param entity the entity being asked about; null yields false
     * @return true when RAT is enabled and the entity is an Abyssal Fracture
     */
    public static boolean isAbyssalFracture(SectorEntityToken entity) {
        // Short-circuit before touching RatTypes so a RAT-free install never
        // loads the class that names AbyssalFracture.
        if (entity == null || !RandomAssortmentOfThingsPresence.isModEnabled()) {
            return false;
        }
        return RatTypes.isAbyssalFracture(entity);
    }

    // Isolates the only reference to a RAT type. The classloader resolves this
    // holder on first call, which the gate in isAbyssalFracture defers until
    // RAT is known to be present, so AbyssalFracture is never sought otherwise.
    private static final class RatTypes {

        private static boolean hasAbyssalFracture(StarSystemAPI system) {
            for (var entity : system.getAllEntities()) {
                if (isAbyssalFracture(entity)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isAbyssalFracture(SectorEntityToken entity) {
            return entity.getCustomPlugin() instanceof AbyssalFracture;
        }
    }
}
