package kmlib.mods.nexerelin;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.DeclinedWork;
import kmlib.extensions.ExecutedWork;
import kmlib.extensions.WorkOutcome;

import exerelin.campaign.SectorManager;

/**
 * Which counters a colony trades over, decided the way Nexerelin decides it, on an install running
 * it.
 *
 * <p>Unlike the founding this package also defers, this is one aspect of an ownership change rather
 * than the whole of one. The mod has a routine for the counters and none for the rest - the flag,
 * the player-owned mark, the outlying entities and the tariff all sit inline in the body of its own
 * transfer - so the counters defer here and everything around them stays as this library states it.
 *
 * <p>What that routine carries and a transcription cannot is knowledge of other mods. It names
 * particular modded markets that trade without a black market, particular ones that keep a military
 * counter whatever they run and particular ones that never do, counts a Templar world and a
 * Tri-Tachyon-style trading post by conditions and industries this library does not know, and swaps
 * the military counter itself for one belonging to another mod on a faction that ships its own. On
 * an install running those mods, those are the right answers and a general rule gives wrong ones.
 *
 * <p>Storage stays outside this. The mod's routine does not name that counter at all, so a caller
 * deferring the whole submarket half to it would silently drop the one thing that has to be true of
 * a colony the player holds - a hold they can reach without being billed for it.
 *
 * <p>The mod is optional, so the sole reference to a type of its own lives in the nested
 * {@link NexerelinTypes} holder, which the classloader does not resolve until the presence gate has
 * passed. That keeps an install without the mod from ever seeking a class it does not have.
 *
 * <p>Nothing outside this package is named here. The routine is wrapped as the mod states it, and
 * which of the two submarket rules an install takes is decided where that choice belongs - beside
 * the rule that is the other half of it - so the two sides depend on this one rather than on each
 * other.
 */
public final class NexerelinSubmarkets {

    private NexerelinSubmarkets() {
        // utility class, no instances.
    }

    /**
     * Brings the colony's counters to what the incoming owner trades over, through Nexerelin's own
     * rule where this install can take that path.
     *
     * <p>Declining is a normal answer rather than a failure: the mod may not be installed. Every
     * decline leaves the counters untouched, so the caller is free to apply its own rule.
     *
     * <p>The outgoing owner is asked for because the mod's routine restocks the counters that
     * participate in the economy when the colony has actually changed hands, and skips the restock
     * when it has not - a colony re-stated under the owner it already had keeping the stock it
     * already carries. Passing the incoming id for both would therefore not fail; it would quietly
     * leave a colony that has changed hands trading the previous owner's stock. An outgoing owner
     * nobody can name reads as a change, which is the answer that restocks rather than the one that
     * silently does not.
     *
     * @param market        the colony whose counters are being brought to the incoming owner's;
     *                      null is declined
     * @param oldOwnerId    the outgoing owner's faction id, read before the incoming one landed;
     *                      null is taken as an owner that cannot be matched
     * @param newOwnerId    the incoming owner's faction id; null is declined, the mod's routine
     *                      reading every one of its verdicts off it
     * @return the counters decided by Nexerelin, or a decline naming what about this call it could
     *         not decide - the counters left exactly as they were either way
     */
    public static WorkOutcome applySubmarkets(
            MarketAPI market,
            String oldOwnerId,
            String newOwnerId) {

        // The presence gate is asked first and alone, so an install without the mod reads nothing
        // else and never reaches the holder below.
        if (!NexerelinPresence.isModEnabled()) {
            return new DeclinedWork("Nexerelin is not enabled on this install");
        }

        if (market == null || newOwnerId == null) {
            return new DeclinedWork("the counters were stated without a colony or an incoming "
                + "owner, and Nexerelin reads every one of its verdicts off the incoming owner");
        }

        NexerelinTypes.updateSubmarkets(market, oldOwnerId, newOwnerId);

        return new ExecutedWork();
    }

    // Isolates the only reference to a Nexerelin type. The classloader resolves this holder on
    // first call, which the presence gate defers until the mod is known to be present.
    private static final class NexerelinTypes {

        private static void updateSubmarkets(
                MarketAPI market,
                String oldOwnerId,
                String newOwnerId) {

            SectorManager.updateSubmarkets(market, oldOwnerId, newOwnerId);
        }
    }
}
