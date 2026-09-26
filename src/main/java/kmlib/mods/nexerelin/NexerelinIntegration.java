package kmlib.mods.nexerelin;

import com.fs.starfarer.api.Global;

import kmlib.KmlibMod;
import kmlib.extensions.FallbackToDefaults;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.ModIntegration;
import kmlib.starsector.markets.colonisation.ColonisationRoutines;
import kmlib.starsector.markets.ownership.OwnerSubmarketRules;
import kmlib.starsector.markets.ownership.OwnershipTransferRoutines;
import kmlib.starsector.strings.KmlibStringKeys;

import org.apache.log4j.Logger;

/**
 * Where this package's adapters are put in front of the operations that may defer to them.
 *
 * <p>It exists so that no operation has to name Nexerelin. What a founding or a hand-over consists
 * of, and the shape of a routine that could take one over, belong to the operation; which routines
 * this particular install has does not, and an operation reaching for a named mod is deciding
 * something it has no view of. Registering from this side leaves the arrow pointing one way -
 * this package knows the operations, the operations know nothing of it - and puts the whole of
 * Nexerelin's presence in this library in one file that can be read in a minute.
 *
 * <p>Whether the mod is there is asked once, here, rather than on every founding. It is a fact
 * about the install and cannot change while the game is up, so an install without Nexerelin
 * registers nothing and its operations then run without a single question about mods being asked
 * anywhere.
 *
 * <p>No sector is captured here, and none is read. Each adapter takes the sector it acts in as an
 * argument of the work itself, so one registration made at load serves whatever campaign is
 * started after it.
 */
public final class NexerelinIntegration {

    private static final Logger LOG = Global.getLogger(NexerelinIntegration.class);

    // What this integration is called in a line about what is founding colonies or moving them on
    // this install. The mod's own name rather than its ID: what a log line is read for here is
    // which mod is doing the work, and that is the name the reader knows it by. Read off the
    // presence beside this rather than spelled again, so this mod is one name wherever it is shown.
    private static final String INTEGRATION_NAME = NexerelinPresence.MOD_NAME;

    // Which of the library's features a failure of these adapters costs, as the half of a latch key
    // the mod ID does not cover.
    private static final String NEXERELIN_ROUTINES_FEATURE_KEY = "nexerelin-routines";

    private NexerelinIntegration() {
        // utility class, no instances.
    }

    /**
     * Registers this package's adapters with the operations they take over, where the mod is
     * enabled.
     *
     * <p>Each operation holds one adapter, so a second call installs the same adapters over the
     * ones this call left - which reads in the log as this integration displacing itself, and is
     * the same install either way.
     */
    public static void installRoutines() {
        installRoutines(NexerelinPresence.isModEnabled());
    }

    /**
     * These adapters as the compatibility channel states them: Nexerelin as the third party, and the
     * library as the mod that loses something by it.
     *
     * <p>One description for the install and for every adapter it registers, so a guard over the
     * install and an adapter failing when first called latch under one binding and are one report.
     * Composed only once something has failed, so the wording read out of strings.json and the mod
     * manager read behind the installed version stay off every path that worked.
     *
     * @return the integration a failure of this install, or of any adapter it registered, is
     *         reported under
     */
    public static ModIntegration describeIntegration() {

        return new ModIntegration(
            NexerelinPresence.MOD_ID,
            NexerelinPresence.MOD_NAME,
            new CompatibilityConsumer(
                KmlibMod.MOD_ID,
                NEXERELIN_ROUTINES_FEATURE_KEY,
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_LOST_NEXERELIN_ROUTINES),
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_UNAFFECTED_NEXERELIN_ROUTINES)));
    }

    // The same installation against a stated answer rather than the live one, which is what lets an
    // install with the mod and one without be posed on a machine that has whichever mods it
    // happens to have.
    static void installRoutines(boolean isNexerelinEnabled) {

        if (!isNexerelinEnabled) {

            LOG.debug("Nexerelin is not enabled.");
            return;
        }

        // Every one of these permits the plainer work in its place, and the colonisation adapter is
        // why it is worth saying rather than assuming. Nexerelin's own founding needs a planet - it
        // renames the body and reads the system off it - so a station or an oddity a mod hung a
        // colonisable market on is handed back, and the composed sequence founds it. Forbidding the
        // fallback here would turn that ordinary case into a failed command.
        ColonisationRoutines.registerRoutine(
            INTEGRATION_NAME,
            NexerelinColoniser::establishColony,
            FallbackToDefaults.PERMITTED,
            NexerelinIntegration::describeIntegration);

        OwnershipTransferRoutines.registerRoutine(
            INTEGRATION_NAME,
            NexerelinMarketTransfer::transferOwnership,
            FallbackToDefaults.PERMITTED,
            NexerelinIntegration::describeIntegration);

        OwnerSubmarketRules.registerRule(
            INTEGRATION_NAME,
            NexerelinSubmarkets::applySubmarkets,
            FallbackToDefaults.PERMITTED,
            NexerelinIntegration::describeIntegration);
    }
}
