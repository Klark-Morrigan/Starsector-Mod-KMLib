package kmlib.starsector.nexerelin;

import com.fs.starfarer.api.Global;

import kmlib.extensions.FallbackToDefaults;
import kmlib.starsector.markets.colonisation.ColonisationRoutines;
import kmlib.starsector.markets.ownership.OwnerSubmarketRules;
import kmlib.starsector.markets.ownership.OwnershipTransferRoutines;

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
    // this install. The mod's own name rather than its id: what a log line is read for here is
    // which mod is doing the work, and that is the name the reader knows it by.
    private static final String INTEGRATION_NAME = "Nexerelin";

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
            FallbackToDefaults.PERMITTED);

        OwnershipTransferRoutines.registerRoutine(
            INTEGRATION_NAME,
            NexerelinMarketTransfer::transferOwnership,
            FallbackToDefaults.PERMITTED);

        OwnerSubmarketRules.registerRule(
            INTEGRATION_NAME,
            NexerelinSubmarkets::applySubmarkets,
            FallbackToDefaults.PERMITTED);
    }
}
