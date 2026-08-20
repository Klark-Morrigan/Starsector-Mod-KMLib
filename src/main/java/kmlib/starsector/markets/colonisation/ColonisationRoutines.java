package kmlib.starsector.markets.colonisation;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.ExtensionPoint;
import kmlib.extensions.FallbackToDefaults;
import kmlib.extensions.WorkOutcome;

/**
 * Which colonisation this install founds colonies with, and the offer made to it before one is
 * founded the game's own way.
 *
 * <p>The point belongs to colonisation rather than to whoever fills it, which is what keeps this
 * package free of the mods it defers to: founding a colony is stated here in terms of a routine
 * that might take it over, and which routine exists is settled where the install is composed. A
 * mod with a colonisation of its own registers here at load, and this package never learns its
 * name.
 *
 * <p>One routine, the last registered. A founding is taken over whole or not at all, so a second
 * routine behind the first would never be reached - and the mod that supplied it would go on being
 * installed while none of its colonisation ever ran. Whoever registers last is therefore the one
 * that founds, and the displacement is logged rather than left to be discovered.
 *
 * <p>A routine that declines, and an install that registered none, both leave the founding to the
 * game's own sequence - which is what every install without such a mod runs.
 *
 * <p>Final class with a private constructor: the point is the state, and it is one point per
 * running game rather than one per holder of a reference to it.
 */
public final class ColonisationRoutines {

    private static final ExtensionPoint<ColonisationRoutine> INSTALLED_ROUTINE =
        new ExtensionPoint<>("colonisation routine");

    private ColonisationRoutines() {
        // utility class, no instances.
    }

    /**
     * Empties the point, so an install can be composed again from nothing.
     */
    public static void clearRoutine() {
        INSTALLED_ROUTINE.clearImplementation();
    }

    /**
     * @return the routine this install founds colonies with, or null where nothing registered one
     */
    public static ColonisationRoutine readRoutine() {
        return INSTALLED_ROUTINE.readImplementation();
    }

    /**
     * @return the name that routine was registered under, or null where nothing is installed
     */
    public static String readRoutineName() {
        return INSTALLED_ROUTINE.readImplementationName();
    }

    /**
     * Installs the colonisation this install founds colonies with, replacing whatever was there.
     *
     * @param integrationName     who is founding colonies now, for the log - a mod's name
     * @param colonisationRoutine the routine to offer foundings to; null is passed over
     * @param fallbackToDefaults  whether a body this routine declines may be founded the game's own
     *                            way instead - forbidden by a mod whose colonies are not the game's
     *                            colonies, where the plainer founding would be wrong rather than
     *                            merely plainer
     */
    public static void registerRoutine(
            String integrationName,
            ColonisationRoutine colonisationRoutine,
            FallbackToDefaults fallbackToDefaults) {

        INSTALLED_ROUTINE.registerImplementation(
            integrationName,
            colonisationRoutine,
            fallbackToDefaults);
    }

    // Offers the founding to whatever is installed, and says whether it was taken. Shaped as one
    // routine itself, so the operation offering a founding asks one question of its own package
    // rather than reading a point it would then have to know the rules of.
    static WorkOutcome offerColonisation(
            SectorAPI sector,
            MarketAPI market,
            String factionId,
            int colonySize) {

        var colonisationRoutine = readRoutine();

        var outcome = colonisationRoutine != null
            ? colonisationRoutine.establishColony(sector, market, factionId, colonySize)
            : null;

        return INSTALLED_ROUTINE.settleWorkOutcome(outcome);
    }
}
