package kmlib.mods.rat;

import com.fs.starfarer.api.Global;

import kmlib.KmlibMod;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.ModIntegration;
import kmlib.starsector.strings.KmlibStringKeys;
import kmlib.starsector.systems.ModdedSystemAccessRoutes;

import org.apache.log4j.Logger;

/**
 * Where this package's adapters are put in front of the reads that may defer to them.
 *
 * <p>It exists so that no read has to name Random Assortment of Things. What it means for a system
 * to be reachable belongs to the read; which means of arrival this particular install has does not,
 * and a read reaching for a named mod is deciding something it has no view of. Registering from
 * this side leaves the arrow pointing one way - this package knows the reads, the reads know
 * nothing of it - and puts the whole of this mod's presence in the library's own operations in one
 * file that can be read in a minute.
 *
 * <p>Whether the mod is there is asked once, here, rather than on every reachability read. It is a
 * fact about the install and cannot change while the game is up, so an install without the mod
 * registers nothing and its reads then run without a single question about mods being asked
 * anywhere.
 *
 * <p>What is registered here is not the whole of this package. The minimap adapter is answered
 * through a role a caller holds rather than through a point the library consults, so it is
 * composed where that caller is composed and needs nothing from this.
 *
 * <p>No sector is captured here, and none is read. The route takes the system it is asked about as
 * an argument of the read itself, so one registration made at load serves whatever campaign is
 * started after it.
 */
public final class RandomAssortmentOfThingsIntegration {

    private static final Logger LOG = Global.getLogger(RandomAssortmentOfThingsIntegration.class);

    // What this integration is called in a line about what makes a system reachable on this
    // install. The mod's name rather than its ID: what such a line is read for is which mod is
    // supplying the way in, and that is the name the reader knows it by. Read off the presence
    // beside this rather than spelled again, so this mod is one name wherever it is shown.
    private static final String INTEGRATION_NAME = RandomAssortmentOfThingsPresence.MOD_NAME;

    // Which of the library's features a failure of this route costs, as the half of a latch key the
    // mod ID does not cover.
    private static final String RAT_ACCESS_ROUTES_FEATURE_KEY = "system-access-routes";

    private RandomAssortmentOfThingsIntegration() {
        // utility class, no instances.
    }

    /**
     * Registers the ways this mod reaches a star system with the reachability read, where the mod
     * is enabled.
     *
     * <p>Routes are keyed by name, so a second call replaces this integration's own route rather
     * than adding a second one beside it - which is the same install either way.
     */
    public static void installModdedSystemAccessRoutes() {
        installModdedSystemAccessRoutes(RandomAssortmentOfThingsPresence.isModEnabled());
    }

    /**
     * This route as the compatibility channel states it: Random Assortment of Things as the third
     * party, and the library as the mod that loses something by it.
     *
     * <p>One description for the install and for the route it registers, so a guard over the
     * install and the route failing when first asked latch under one binding and are one report.
     * Composed only once something has failed, so the wording read out of strings.json and the mod
     * manager read behind the installed version stay off every path that worked.
     *
     * @return the integration a failure of this install, or of the route it registered, is reported
     *         under
     */
    public static ModIntegration describeIntegration() {

        return new ModIntegration(
            RandomAssortmentOfThingsPresence.MOD_ID,
            RandomAssortmentOfThingsPresence.MOD_NAME,
            new CompatibilityConsumer(
                KmlibMod.MOD_ID,
                RAT_ACCESS_ROUTES_FEATURE_KEY,
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_LOST_RAT_ACCESS_ROUTES),
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_UNAFFECTED_RAT_ACCESS_ROUTES)));
    }

    // The same installation against a stated answer rather than the live one, which is what lets an
    // install with the mod and one without be posed on a machine that has whichever mods it
    // happens to have.
    static void installModdedSystemAccessRoutes(boolean isRandomAssortmentOfThingsEnabled) {

        if (!isRandomAssortmentOfThingsEnabled) {

            LOG.debug("Random Assortment of Things is not enabled.");
            return;
        }

        // The Abyssal Fracture moves fleets in with a manual hyperspace transition rather than a
        // jump point, so a system reached only through one holds no jump point and may carry the
        // cut-off tag while being perfectly reachable. Registered as a route, that is the whole of
        // what the reachability read needs to know about this mod.
        //
        // The fracture also satisfies the other half of what a route vouches for: this mod gives it
        // a map icon of its own, so the player sees the way in marked where it is, and nothing has
        // to find a star drawn for a destination that was never given one.
        ModdedSystemAccessRoutes.registerRoute(
            INTEGRATION_NAME,
            RandomAssortmentOfThingsMatcher::hasAbyssalFracture,
            RandomAssortmentOfThingsIntegration::describeIntegration);
    }
}
