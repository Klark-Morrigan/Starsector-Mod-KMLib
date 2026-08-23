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
 * <p>Nothing here is called known, for that reason. What the player may be told about a
 * colony is discovery <em>and</em> a revelation the system decides, so a name promising
 * knowledge stated over a market alone would promise more than it could deliver - and a
 * caller reading it would believe the fog had been applied in full.
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
     * Whether a market counts as an owned colony the player has found - the base fog every
     * rule about showing a colony starts from.
     *
     * <p>Composes {@link Markets#isOwnedColony} with {@link #isDiscoveredByPlayer} so the pair
     * means one thing across every caller rather than each re-deriving it and drifting. The
     * reveal drops the discovery arm, admitting a colony the player has not yet found so an
     * undiscovered faction still paints under it.
     *
     * <p>Discovery is the whole of the fog. A market's public listing is not a second arm of
     * it, because a colony listed in an economy the player has no sight of is not something
     * the player has any way of knowing about - and admitting it would show every undiscovered
     * market that merely omits to hide itself, every derelict station in the sector among
     * them, from the first frame of a campaign. The case such an arm would be admitting for, a
     * colony surfaced by a story reveal ahead of a fleet reaching it, is one vanilla marks
     * found by clearing the entity's discoverable flag, which this already reads.
     *
     * <p>The base fog rather than the whole rule: the kinds of colony that would leak on
     * discovery alone - a derelict station, a colony concealing itself - are held back by a
     * further gate stated over a colony set, where the containing system can be read.
     *
     * @param market                           the market to test; null yields false
     * @param shouldIncludeUndiscoveredMarkets whether an undiscovered colony still counts (the
     *                                         "show all factions" reveal); false applies the
     *                                         discovery filter
     * @return true when a faction owns the market and it is either found or the reveal is on
     */
    public static boolean isCountedAsColony(
            MarketAPI market,
            boolean shouldIncludeUndiscoveredMarkets) {

        if (!Markets.isOwnedColony(market)) {
            return false;
        }
        return shouldIncludeUndiscoveredMarkets || isDiscoveredByPlayer(market);
    }

    /**
     * Whether a market counts as a dead world the player has seen is dead - the base fog for the
     * one kind of colony whose finding is a survey rather than a discovery.
     *
     * <p>The counterpart of {@link #isCountedAsColony} for a ruin, and it has to be one rather
     * than a case of it: a decivilised world is stripped of its owner as it dies, so the ownership
     * arm above refuses it outright, and being condition-only is what it <em>is</em> rather than a
     * reason to withhold it.
     *
     * <p>Two arms, both of which must admit it, because they answer different questions. The
     * survey arm asks whether anybody has looked closely enough to read the ruins; the discovery
     * arm asks whether the planet has been found at all. A world flown past but never surveyed
     * fails the first and passes the second, which is exactly the case each reveal exists to be
     * asked about separately - so each reveal drops its own arm and neither drops the other's.
     *
     * @param market                           the market to test; null yields false
     * @param shouldIncludeUndiscoveredMarkets whether a market on an undiscovered entity still
     *                                         counts; false applies the discovery filter
     * @param shouldIncludeUnsurveyedDeadWorlds whether an unsurveyed ruin still counts; false
     *                                         applies the survey filter
     * @return true when the market is a dead world and both arms admit it
     */
    public static boolean isCountedAsDeadColony(
            MarketAPI market,
            boolean shouldIncludeUndiscoveredMarkets,
            boolean shouldIncludeUnsurveyedDeadWorlds) {

        if (!DecivilisedMarkets.isDecivilisedWorld(market)) {
            return false;
        }
        if (!shouldIncludeUnsurveyedDeadWorlds
                && !DecivilisedMarkets.isRevealedDecivilised(market)) {
            return false;
        }
        return shouldIncludeUndiscoveredMarkets || isDiscoveredByPlayer(market);
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
     * <p>Neither the owner nor the intel directory is read, so a faction hidden from the
     * directory is not barred, and a concealed base on an entity the player has found
     * (a raided pirate base) reads found.
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
}
