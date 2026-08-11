package kmlib.starsector.systems.claims;

import kmlib.starsector.entities.EntityNameplate;

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
 * @param marketNameplate     how the colony is identified to a reader - its name and the glyph the
 *                           sector map marks it with. Recorded on the walk that met the colony
 *                           rather than looked up again by whatever draws the name, so the pair
 *                           shown can only ever belong to the colony whose score is stated beside
 *                           it
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
 * @param admission          how the mechanic's walk met the market - as a competitor it weighed,
 *                           or as one it carried without weighing. Independent of
 *                           {@link #isKnownToPlayer}, which is about the player rather than the
 *                           mechanic: a discovered base is known and concealed at once
 * @param marketSize         the colony's own size rating, the term the score starts from
 * @param siblingMarketCount how many other markets the same faction holds in the system, each
 *                           worth a point - so the count is the term. Taken over every market
 *                           present - hidden and player-owned alike - because sheer presence is
 *                           what it measures, not who is eligible to claim
 * @param militaryBonus      the flat bonus a garrison earns, present only for a military market
 */
public record MarketClaimBreakdown(
    EntityNameplate marketNameplate,
    int listingPosition,
    boolean isKnownToPlayer,
    ContestAdmission admission,
    int marketSize,
    int siblingMarketCount,
    OptionalInt militaryBonus) {

    // What a market that is no garrison adds on the military term.
    private static final int NO_MILITARY_BONUS = 0;

    /**
     * Reads a bonus handed over as null as no bonus and an unstated admission as the weighed one,
     * so a hand-built market cannot fail late on either. The nameplate looks after its own unstated
     * half.
     */
    public MarketClaimBreakdown {
        militaryBonus = militaryBonus == null ? OptionalInt.empty() : militaryBonus;
        admission = admission == null ? ContestAdmission.WEIGHED : admission;
    }

    /**
     * Whether the mechanic weighed this market as a competitor in its own right.
     *
     * @return true when the market competed on its own account
     */
    public boolean isScoredOnItsOwnAccount() {
        return admission.isScoredOnItsOwnAccount();
    }

    /**
     * Whether the market is concealed rather than held in the open.
     *
     * @return true when the mechanic skipped it before scoring
     */
    public boolean isHiddenMarket() {
        return admission.isHiddenMarket();
    }

    /**
     * Whether the colony sits outside the economy's own listing.
     *
     * @return true when the mechanic's walk never reached it
     */
    public boolean isOffEconomyMarket() {
        return admission.isOffEconomyMarket();
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
