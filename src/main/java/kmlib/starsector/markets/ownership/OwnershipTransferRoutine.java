package kmlib.starsector.markets.ownership;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

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
 * <p>One method that answers whether it acted, rather than a question and a command. A routine
 * declines for a reason the caller has an answer to - the mod is not installed, or the colony flies
 * no flag the routine can read the outgoing owner off - and reporting the decline is what lets the
 * composed sequence run in its place instead of a colony staying with the owner it was being taken
 * from. Split in two, the pair would also be open to a caller asking and then not calling, which is
 * that same colony.
 *
 * <p>Whether the hand-over is a real one is settled before a routine is offered it. A colony named
 * for the owner it already has is refused by the operation itself, so a routine here is only ever
 * handed a change of owner.
 *
 * <p>Deliberately not part of the library's public surface. It is the seam that lets the branch be
 * posed both ways without a mod installed, not an extension point: a routine is bound here because
 * this library knows how to defer to that mod, and a caller supplying its own would be choosing a
 * hand-over the rest of the library cannot reason about.
 */
@FunctionalInterface
interface OwnershipTransferRoutine {

    /**
     * Hands the colony to its new owner if this routine is the one that should, and says whether it
     * did.
     *
     * <p>Only the incoming owner is stated. The outgoing one is whoever the colony still reads as
     * belonging to, nothing having been changed about it yet, so passing it as well would let a
     * caller name an owner the colony never had.
     *
     * @param market    the colony changing hands
     * @param factionId the incoming owner's faction id
     * @return true when this routine handed the colony over and nothing further is to be done to
     *         it; false when it declined, leaving the colony exactly as it was for the caller to
     *         hand over itself
     */
    boolean transferOwnership(MarketAPI market, String factionId);
}
