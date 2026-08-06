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
 * @param marketSize         the colony's own size rating, the term the score starts from
 * @param siblingMarketCount how many other markets the same faction holds in the system, each
 *                           worth one point. Counted over every market present - hidden and
 *                           player-owned alike - because sheer presence is what it measures,
 *                           not who is eligible to claim
 * @param militaryBonus      the flat bonus a garrison earns, present only for a military market
 */
public record MarketClaimBreakdown(
    String marketName,
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
     * What the market is worth in the contest: its size, its siblings and its garrison bonus.
     *
     * @return the market's claim score
     */
    public int computeTotalScore() {
        return marketSize + siblingMarketCount + militaryBonus.orElse(NO_MILITARY_BONUS);
    }
}
