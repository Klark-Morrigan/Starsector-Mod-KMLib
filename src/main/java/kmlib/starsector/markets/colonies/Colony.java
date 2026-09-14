package kmlib.starsector.markets.colonies;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.MarketVisibility;

import java.util.Objects;

/**
 * One colony: its market, and whether the sector's economy lists it.
 *
 * <p>Carries no location. Where a colony was found is the caller's own knowledge, because the
 * caller is what asked - it named a star system, or hyperspace, or the whole sector, and got
 * back what was there. Storing the place here would be a second copy of that, free to disagree
 * with it, and would make a colony unusable in any listing that spans more than one place. A
 * caller that does need the location asks the market for it.
 *
 * <p>A colony can sit on a real entity under a real faction and never be registered with the
 * economy - vanilla builds Galatia Academy that way. Which listing a market was found in is
 * therefore real information, and it is destroyed the moment the two listings are
 * concatenated. A reader that weighs a colony needs it, since weight is computed from
 * industries, conditions and stability the economy is what maintains; a reader that merely
 * counts or names one does not. Carrying it lets both read one set rather than each walking
 * the listing that suits it and then disagreeing about what is present.
 *
 * <p>Concealment, discovery and the owner are all read back off the market rather than stored
 * beside it, so a colony can never report a state its own market contradicts. Listing membership
 * has no such source to defer to - it is a property of where the market was found, not of the
 * market - which is exactly why it is stored here.
 *
 * <p>Those reads are why the market is required rather than absorbed when absent. A colony with
 * no market has no concealment, no discovery and no owner, so every one of them would have to
 * invent an answer; refusing the colony at construction fails where the mistake is instead of
 * three reads downstream, each free to invent a different one.
 *
 * <p>Says nothing about what sort of place the market stands for, nor about what the player may
 * be told of it. Both are classifications drawn for a purpose rather than facts the sector holds,
 * so they belong to whatever is drawing them - a caller that needs them states them over this set.
 *
 * @param market            the colony's market; mandatory, a colony with no market being no
 *                          colony at all - a null one is refused rather than absorbed
 * @param isListedByEconomy whether the sector's economy lists this market, as opposed to it
 *                          hanging on one of its location's entities unregistered
 */
public record Colony(
    MarketAPI market,
    boolean isListedByEconomy) {

    /** Refuses a colony with no market, there being nothing for its own reads to answer off. */
    public Colony {
        Objects.requireNonNull(market, "A colony needs a market: it has no facts without one.");
    }

    /**
     * Whether the colony is concealed - present and owned, but not publicly listed.
     *
     * <p>Independent of whether the player has found it: a raided pirate base stays permanently
     * hidden while being perfectly well known.
     *
     * @return true when the market is hidden
     */
    public boolean isHidden() {
        return market.isHidden();
    }

    /**
     * Whether the player has found this colony's market - the entity's own fact, and the base
     * every rule about showing a colony is built on.
     *
     * <p>Not the whole of what the player may be told. A caller withholding what the player could
     * not plausibly have heard of composes further conditions over this one, and needs the place
     * the colony stands in to do it - which is why no such composition is offered here.
     *
     * @return true when the player has found the market's entity
     */
    public boolean isDiscoveredByPlayer() {
        return MarketVisibility.isDiscoveredByPlayer(market);
    }

    /**
     * Who holds this colony, as the faction ID an ownership change writes.
     *
     * <p>Read off the market's own ID rather than its faction object, that being what a transfer
     * writes and therefore what answers for the owner a moment after one.
     *
     * @return the owning faction's ID, or null where the market names none
     */
    public String readOwnerId() {
        return market.getFactionId();
    }
}
