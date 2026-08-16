package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.Markets;

/**
 * One colony in a star system, paired with the single fact about it that cannot be recovered
 * from the market alone: whether the sector's economy lists it.
 *
 * <p>A colony can sit on a real entity under a real faction and never be registered with the
 * economy - vanilla builds Galatia Academy that way. Which listing a market was found in is
 * therefore real information, and it is destroyed the moment the two listings are
 * concatenated. A reader that weighs a colony needs it, since weight is computed from
 * industries, conditions and stability the economy is what maintains; a reader that merely
 * counts or names one does not. Carrying it lets both read one set rather than each walking
 * the listing that suits it and then disagreeing about what is present.
 *
 * <p>Concealment and discovery are answered through {@link Markets} rather than stored beside
 * the market, so a colony can never report a state its own market contradicts. Listing
 * membership has no such source to defer to - it is a property of where the market was found,
 * not of the market - which is exactly why it is the one fact stored here.
 *
 * @param market            the colony's market; mandatory, a colony with no market being no
 *                          colony at all
 * @param isListedByEconomy whether the sector's economy lists this market, as opposed to it
 *                          hanging on one of the system's entities unregistered
 */
public record SystemColony(
    MarketAPI market,
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
     * @return true when the market is discovered or has been surfaced into the open
     */
    public boolean isKnownToPlayer() {
        return Markets.isKnownToPlayer(market);
    }
}
