package kmlib.starsector.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.SectorAPI;

import java.util.function.Supplier;

/**
 * Registering and clearing the sector's per-frame scripts, so every installer states what it wants
 * rather than writing the null guard and the walk for itself.
 *
 * <p>Two registrations, for the two lifetimes a script can have, and the choice is the caller's to
 * make on purpose: a script that enters the save is restored beside whatever the next load adds,
 * while a transient one is gone with the session.
 *
 * <p>{@link #addIfAbsent} is for the persisted list. It keeps the registration already there rather
 * than replacing it, because a persisted script is carrying state - idempotency stamps,
 * accumulators - that a replacement would throw away.
 *
 * <p>{@link #installTransientScript} is for a script that must be fresh per load and never
 * serialised: it clears every registration of the built script's own class first, then adds.
 * A presence check that kept what was there would be no safer than replacing it - both leave
 * exactly one - and would be worse for a script holding the sector it was built against. That is
 * what leaves {@link #removeTransientScripts} needed only where a feature can be switched off
 * mid-session: across a load there is nothing left to remove.
 *
 * <p>The class cleared is the built script's own rather than one the caller names beside it - a
 * caller that could name it could name a different one, and what that buys is a pass silently
 * doubling while a sibling disappears - which is why the transient shape differs from
 * {@link kmlib.starsector.listeners.SectorListeners}, where the two are separate calls on the
 * engine's manager. The engine clears by exact class - {@code getClass() != clazz}, no subtype - so
 * two scripts that must be installed and taken back independently have to be two classes, and a
 * script whose class a sibling mod may also be running over the same sector is the wrong fit for
 * the pair at all: {@link InstalledTransientScript} is that case, and states the difference.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 */
public final class SectorScripts {

    private SectorScripts() {
        // utility class, no instances.
    }

    /**
     * Adds a freshly built instance of {@code scriptClass} to the sector's persisted script list,
     * if and only if no script there is already an instance of that class.
     *
     * <p>Subclasses count as instances, the same reading the engine's own by-class removal of
     * persisted scripts applies.
     *
     * @param sector      the sector to install on; null is a no-op
     * @param scriptClass the class checked for, passed explicitly rather than reflected off the
     *                    supplier because erasure makes a lambda's runtime type unreliable
     * @param factory     called only when no script of {@code scriptClass} is present, so the
     *                    common skip path allocates nothing
     * @param <T>         the concrete script type
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

    /**
     * Adds one script to the sector as a transient one, clearing every registration of its class
     * first.
     *
     * @param sector      the sector to register with; null is a no-op
     * @param buildScript called only once there is a sector to add to, so a load that cannot
     *                    register does not construct a script it would discard
     */
    public static void installTransientScript(
            SectorAPI sector,
            Supplier<? extends EveryFrameScript> buildScript) {

        if (sector == null) {
            return;
        }
        var installedScript = buildScript.get();

        sector.removeTransientScriptsOfClass(installedScript.getClass());
        sector.addTransientScript(installedScript);
    }

    /**
     * Clears every transient registration of one script class, adding nothing back - what a
     * feature switched off asks for, so a pass does not go on running for a feature the player has
     * turned off.
     *
     * @param sector      the sector to clear on; null is a no-op
     * @param scriptClass the class every registration of is cleared
     */
    public static void removeTransientScripts(
            SectorAPI sector,
            Class<? extends EveryFrameScript> scriptClass) {

        if (sector == null) {
            return;
        }
        sector.removeTransientScriptsOfClass(scriptClass);
    }
}
