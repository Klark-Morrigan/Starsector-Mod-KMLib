package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

/**
 * What the player may be told about a market.
 *
 * <p>Separate from {@link Markets}, which answers what a market <em>is</em>. These
 * answer what may be said about it, and the difference is not cosmetic: a caller
 * reaching for a state read wants the market's own facts however the fog stands, while
 * a caller reaching for one of these is asking permission to draw or name something.
 * Sharing one class let a display read a state predicate by mistake and paint a colony
 * nobody has found.
 *
 * <p>Only the market's own facts are in reach here. A rule needing the containing
 * system - has the player been in it, does anybody live in it - cannot be stated from a
 * market alone and belongs where the system is in hand.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state.
 * Matches the shape of its neighbours in this package, and is null-market defensive
 * like the rest of the library.
 */
public final class MarketVisibility {

    private MarketVisibility() {
        // utility class, no instances.
    }

    /**
     * Whether a market counts as a known owned colony - the "counts on the map" filter a
     * presence or dominance read admits a market by.
     *
     * <p>Composes {@link Markets#isOwnedColony} with {@link #isKnownToPlayer} so "counts as a
     * known colony" means one thing across every caller rather than each re-deriving the pair
     * and drifting. The dev reveal drops the visibility arm, admitting a colony the player has
     * not yet found so an undiscovered faction still paints under it.
     *
     * @param market                           the market to test; null yields false
     * @param shouldIncludeUndiscoveredMarkets whether an undiscovered colony still counts (the
     *                                         "show all factions" dev reveal); false applies the
     *                                         normal known-to-player filter
     * @return true when a faction owns the market and it is either known or the reveal is on
     */
    public static boolean isCountedAsColony(
            MarketAPI market,
            boolean shouldIncludeUndiscoveredMarkets) {

        if (!Markets.isOwnedColony(market)) {
            return false;
        }
        return shouldIncludeUndiscoveredMarkets || isKnownToPlayer(market);
    }

    /**
     * Whether the player has physically found this market's entity.
     *
     * <p>Discovery lives on the entity, not the market: an entity stops being
     * {@code discoverable} once found. A market with no entity reads discovered - there is
     * nothing left to find, so nothing to withhold.
     *
     * <p>Hiddenness is a separate axis and is deliberately not read here. A concealed base
     * the player has raided is discovered and permanently hidden; a colony surfaced by a
     * story reveal is un-hidden while its entity may be either. A caller asking what the
     * player has found must get that answer alone, and a caller weighing concealment reads
     * {@code isHidden} for itself.
     *
     * @param market the market to test; null yields false
     * @return true when the market's entity has been found, or it has no entity
     */
    public static boolean isDiscoveredByPlayer(MarketAPI market) {
        if (market == null) {
            return false;
        }
        var entity = market.getPrimaryEntity();
        return entity == null || !entity.isDiscoverable();
    }

    /**
     * Whether the player knows this market exists - the base fog every display of a
     * colony asks permission of.
     *
     * <p>Knowledge is discovery and nothing besides: the player has found the market's
     * entity. A market's public listing is not an arm of the rule, because a colony
     * listed in an economy the player has no sight of is not something the player has
     * any way of knowing about, and admitting it shows every undiscovered market that
     * merely omits to hide itself - every derelict station in the sector among them -
     * from the first frame of a campaign. The case the listing would be admitting for,
     * a colony surfaced by a story reveal ahead of a fleet reaching it, is one vanilla
     * marks found by clearing the entity's discoverable flag, which this rule already
     * reads.
     *
     * <p>Named apart from {@link #isDiscoveredByPlayer} because the two answer
     * different questions: this is the fog a display asks permission of, that is the
     * fact about the entity the fog is made of. A caller weighing whether it may show
     * something reads this; a caller wanting the entity's own state reads that.
     *
     * <p>Neither reads the owner or the intel directory, so a faction hidden from the
     * directory is not barred, and a concealed base on an entity the player has found
     * (a raided pirate base) reads known.
     *
     * @param market the market to test; null yields false
     * @return true when the player knows the market exists
     */
    public static boolean isKnownToPlayer(MarketAPI market) {
        return isDiscoveredByPlayer(market);
    }
}
