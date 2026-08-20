package kmlib.starsector.markets.ownership;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.ExtensionPoint;
import kmlib.extensions.FallbackToDefaults;
import kmlib.extensions.WorkOutcome;

/**
 * Which hand-over this install moves colonies with, and the offer made to it before one changes
 * hands the way this library would move it.
 *
 * <p>The point belongs to ownership rather than to whoever fills it, which is what keeps this
 * package free of the mods it defers to: handing a colony over is stated here in terms of a routine
 * that might take it over, and which routine exists is settled where the install is composed.
 *
 * <p>One routine, the last registered. A colony changes hands once, so a second routine behind the
 * first would never be reached - and the mod that supplied it would go on being installed while
 * none of its diplomacy ever ran on a transfer. Whoever registers last is therefore the one that
 * moves the colony, and the displacement is logged rather than left to be discovered.
 *
 * <p>A routine that declines, and an install that registered none, both leave the hand-over to the
 * sequence this library composes.
 *
 * <p>Final class with a private constructor: the point is the state, and it is one point per
 * running game rather than one per holder of a reference to it.
 */
public final class OwnershipTransferRoutines {

    private static final ExtensionPoint<OwnershipTransferRoutine> INSTALLED_ROUTINE =
        new ExtensionPoint<>("ownership transfer routine");

    private OwnershipTransferRoutines() {
        // utility class, no instances.
    }

    /**
     * Empties the point, so an install can be composed again from nothing.
     */
    public static void clearRoutine() {
        INSTALLED_ROUTINE.clearImplementation();
    }

    /**
     * @return the routine this install moves colonies with, or null where nothing registered one
     */
    public static OwnershipTransferRoutine readRoutine() {
        return INSTALLED_ROUTINE.readImplementation();
    }

    /**
     * @return the name that routine was registered under, or null where nothing is installed
     */
    public static String readRoutineName() {
        return INSTALLED_ROUTINE.readImplementationName();
    }

    /**
     * Installs the hand-over this install moves colonies with, replacing whatever was there.
     *
     * @param integrationName          who is moving colonies now, for the log - a mod's name
     * @param ownershipTransferRoutine the routine to offer hand-overs to; null is passed over
     * @param fallbackToDefaults       whether a hand-over this routine declines may be made the
     *                                 plain way instead - forbidden by a mod whose colonies cannot
     *                                 change hands without its own standing and intel moving with
     *                                 them
     */
    public static void registerRoutine(
            String integrationName,
            OwnershipTransferRoutine ownershipTransferRoutine,
            FallbackToDefaults fallbackToDefaults) {

        INSTALLED_ROUTINE.registerImplementation(
            integrationName,
            ownershipTransferRoutine,
            fallbackToDefaults);
    }

    // Offers the hand-over to whatever is installed, and says whether it was taken. Shaped as one
    // routine itself, so the operation handing a colony over asks one question of its own package
    // rather than reading a point it would then have to know the rules of.
    static WorkOutcome offerTransfer(MarketAPI market, String factionId) {

        var ownershipTransferRoutine = readRoutine();

        var outcome = ownershipTransferRoutine != null
            ? ownershipTransferRoutine.transferOwnership(market, factionId)
            : null;

        return INSTALLED_ROUTINE.settleWorkOutcome(outcome);
    }
}
