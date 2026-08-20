package kmlib.starsector.markets.ownership;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.WorkOutcome;

/**
 * A hand-over of a colony belonging to something other than this library - the routine an installed
 * mod moves its own colonies between owners with, offered a hand-over before the sequence this
 * library composes is run.
 *
 * <p>It exists because changing who holds a colony is more than rewriting a flag once a mod is
 * running its own diplomacy. Such a mod moves standing between the factions involved, files the
 * hand-over where its players can read it, re-posts the people holding captured offices, re-checks
 * which of its own missions the place is still a target for, and tells the listeners it keeps of its
 * own. None of that can be arranged after the fact by something that only knows the market, so where
 * such a routine is present it takes the whole hand-over rather than having ours layered under it.
 *
 * <p>What a routine here declines for: the mod is not installed, or the colony flies no flag the
 * routine can read the outgoing owner off. Both leave the colony exactly as it was, so the composed
 * sequence hands it over in its place rather than the place staying with the owner it was being
 * taken from.
 *
 * <p>Whether the hand-over is a real one is settled before a routine is offered it. A colony named
 * for the owner it already has is refused by the operation itself, so a routine here is only ever
 * handed a change of owner.
 */
@FunctionalInterface
public interface OwnershipTransferRoutine {

    /**
     * Hands the colony to its new owner if this routine is the one that should, and says whether it
     * did.
     *
     * <p>Only the incoming owner is stated. The outgoing one is whoever the colony still reads as
     * belonging to, nothing having been changed about it yet, so passing it as well would let a
     * caller name an owner the colony never had.
     *
     * @param sector    the sector the colony sits in, and the one the incoming owner's id is
     *                  resolved against
     * @param market    the colony changing hands
     * @param factionId the incoming owner's faction id
     * @return a performed hand-over, or a decline saying what about this call it could not do -
     *         which the caller reads back to whoever is diagnosing the install
     */
    WorkOutcome transferOwnership(SectorAPI sector, MarketAPI market, String factionId);
}
