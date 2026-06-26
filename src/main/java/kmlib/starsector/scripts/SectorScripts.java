package kmlib.starsector.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorAPI;

import java.util.List;
import java.util.function.Supplier;

/**
 * Helpers for working with {@link SectorAPI#getScripts()}. Starsector
 * exposes the script list but offers no built-in "install once,
 * survive reload" primitive — every mod that wants idempotent
 * script registration on {@code onGameLoad} ends up writing the
 * same {@code any { isInstance }} dedup walk. Centralised here so
 * every KM* mod (and any external caller that wants the same
 * shape) shares one implementation.
 *
 * <p>Final class with a private constructor: pure-function
 * utility, no instance state. Matches
 * {@link kmlib.starsector.time.StarsectorClock}'s shape.
 */
public final class SectorScripts {

    private SectorScripts() {
        // utility class, no instances.
    }

    /**
     * Adds a freshly-constructed instance of {@code scriptClass} to
     * {@code sector}'s script list iff no existing script is an
     * instance of that class. Per-class dedup (rather than
     * {@code removeScriptsOfClass} + add) preserves any persisted
     * state the script carries (idempotency stamps, accumulators,
     * etc.) across the call.
     *
     * <p>The {@code factory} is invoked lazily so the (common) skip
     * path does not allocate a throwaway instance.
     *
     * <p>Silently no-ops when {@code sector} is {@code null} — same
     * defensive shape as the rest of the library's
     * {@code Global.getSector()}-aware helpers.
     *
     * @param sector the sector to install on; null is a no-op.
     * @param scriptClass class used for the dedup check; passed
     *     explicitly rather than reflected off the supplier because
     *     erasure makes the lambda's runtime type unreliable.
     * @param factory called only when no existing script of
     *     {@code scriptClass} is present.
     * @param <T> the concrete script type.
     */
    public static <T extends EveryFrameScript> void addIfAbsent(
            SectorAPI sector,
            Class<T> scriptClass,
            Supplier<T> factory) {
        if (sector == null) {
            return;
        }
        var scripts = sector.getScripts();
        if (scripts != null) {
            for (var existing : scripts) {
                if (scriptClass.isInstance(existing)) {
                    return;
                }
            }
        }
        sector.addScript(factory.get());
    }
}
