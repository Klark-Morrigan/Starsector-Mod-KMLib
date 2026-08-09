package kmlib.starsector.systems.claims;

import java.util.OptionalInt;

/**
 * The arithmetic behind one market's weight in a star system's claim contest: the three terms
 * the mechanic adds up, kept apart so a reader can be shown why a colony stands where it does
 * rather than only that it does.
 *
 * <p>The scalar the contest is settled on is the sum <em>over</em> this value rather than a
 * second computation beside it, so the parts and the total can never disagree - which is what
 * lets an explanation built from the parts stand beside the map fill painted from the total.
 *
 * <p>An absent military bonus is a market that is no garrison at all, not one whose garrison
 * was worth nothing: the mechanic's bonus is a flat non-zero constant, so a zero would
 * otherwise read as two different markets at once.
 *
 * <p>Plain values with no Starsector types, so a whole contest is built and read on hand-made
 * inputs.
 *
 * @param marketName         the colony's display name
 * @param listingPosition    where the market falls among the system's owned markets, counting from
 *                           one: the economy's own in the order it lists them, then any market it
 *                           does not list. Carried because the contest is settled on a strictly
 *                           greater score, so two markets that tie are separated by nothing but
 *                           this - the earlier-listed one wins - and an explanation with no way to
 *                           state it can only report a tied outcome as arbitrary
 * @param isKnownToPlayer    whether the player knows this colony exists at all. The mechanic
 *                           itself never asks - it settles a contest over colonies nobody has
 *                           found - so the answer is carried rather than applied, leaving an
 *                           explanation free to withhold what the map has no business naming
 *                           while the claim it explains stays vanilla's
 * @param isHiddenMarket     whether the market is concealed rather than held in the open. A
 *                           hidden market never competes on its own account - the mechanic skips
 *                           it, so it takes no standing and can neither win nor lose a listing
 *                           tie - while still counting toward the sibling term. Independent of
 *                           {@link #isKnownToPlayer}: a discovered base is known and hidden at
 *                           once
 * @param isOffEconomyMarket whether the colony sits outside the economy's own listing - a real
 *                           market on a real entity that was never registered, as vanilla builds
 *                           Galatia Academy. The mechanic walks the economy and nothing else, so
 *                           such a market never enters the contest at all: it takes no standing,
 *                           and unlike a hidden one it does not even reach the sibling term.
 *                           Independent of {@link #isHiddenMarket}: the Academy is both at once,
 *                           a raided pirate base is hidden and listed, and a mod's unregistered
 *                           market need not be hidden at all
 * @param marketSize         the colony's own size rating, the term the score starts from
 * @param siblingMarketCount how many other markets the same faction holds in the system, each
 *                           worth a point - so the count is the term. Taken over every market
 *                           present - hidden and player-owned alike - because sheer presence is
 *                           what it measures, not who is eligible to claim
 * @param militaryBonus      the flat bonus a garrison earns, present only for a military market
 */
public record MarketClaimBreakdown(
    String marketName,
    int listingPosition,
    boolean isKnownToPlayer,
    boolean isHiddenMarket,
    boolean isOffEconomyMarket,
    int marketSize,
    int siblingMarketCount,
    OptionalInt militaryBonus) {

    // What a market that is no garrison adds on the military term.
    private static final int NO_MILITARY_BONUS = 0;

    /** Reads a bonus handed over as null as no bonus, so a hand-built market cannot fail late. */
    public MarketClaimBreakdown {
        militaryBonus = militaryBonus == null ? OptionalInt.empty() : militaryBonus;
    }

    /**
     * Whether the mechanic weighed this market as a competitor in its own right.
     *
     * <p>Two kinds of market are carried in a contest without ever taking part in it: one held in
     * concealment, which the walk skips before scoring, and one the economy does not list, which
     * the walk never reaches. Nothing reading a finished contest needs to tell those apart - both
     * counted for nothing - so the question is asked once here rather than as a pair of tests
     * every reader has to remember to keep in step.
     *
     * @return true when the market competed on its own account
     */
    public boolean isScoredOnItsOwnAccount() {
        return !isHiddenMarket && !isOffEconomyMarket;
    }

    /**
     * What the market is worth in the contest: its size, its siblings and its garrison bonus.
     *
     * @return the market's claim score
     */
    public int computeTotalScore() {
        return marketSize + siblingMarketCount + militaryBonus.orElse(NO_MILITARY_BONUS);
    }
}
