package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.MarketVisibility;

/**
 * One colony: its market, what kind of place that market stands for, and whether the sector's
 * economy lists it.
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
 * <p>Concealment and discovery are answered through {@link MarketVisibility} rather than stored
 * beside the market, so a colony can never report a state its own market contradicts. Listing
 * membership has no such source to defer to - it is a property of where the market was found,
 * not of the market - which is exactly why it is stored here.
 *
 * <p>Kind is stored for the other reason: it is recoverable from the market, but every reader
 * routes on it, and one resolving it for itself is a second statement of what an abandoned
 * station is, free to disagree with this one. Resolving it where the colony is selected also
 * settles it on the market that won its place, rather than on whichever market a later reader
 * happens to hold.
 *
 * @param market            the colony's market; mandatory, a colony with no market being no
 *                          colony at all
 * @param kind              what kind of place the market stands for - somewhere people live, or
 *                          a derelict nobody ever lived on
 * @param isListedByEconomy whether the sector's economy lists this market, as opposed to it
 *                          hanging on one of its location's entities unregistered
 */
public record Colony(
    MarketAPI market,
    ColonyKind kind,
    boolean isListedByEconomy) {

    /**
     * Whether the colony is concealed - present and owned, but not publicly listed.
     *
     * <p>Independent of whether the player has found it: a raided pirate base stays
     * permanently hidden while being perfectly well known. Callers that want the visibility
     * question want {@link #isKnownToPlayer} instead.
     *
     * @return true when the market is hidden
     */
    public boolean isHidden() {
        return market.isHidden();
    }

    /**
     * Whether the player knows this colony exists - the fog question the known projection over
     * a colony set is built on.
     *
     * @return true when the player has found the market's entity
     */
    public boolean isKnownToPlayer() {
        return MarketVisibility.isKnownToPlayer(market);
    }
}
