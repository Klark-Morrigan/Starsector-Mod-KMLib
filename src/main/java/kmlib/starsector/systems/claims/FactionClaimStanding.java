package kmlib.starsector.systems.claims;

import java.util.List;

/**
 * One faction's place in a star system's claim contest, of which there are exactly two kinds: a
 * presence the mechanic weighed, and a presence it never reached.
 *
 * <p>Sealed because the two differ in what they can be asked. A weighed standing rests on a single
 * market the mechanic scored, and any reader explaining a score has to reach that market; a
 * presence-only standing has no such market by definition - every colony behind it is one the walk
 * skipped or never met - so asking it what it stands on is a question with no answer. Stated in the
 * type, that question cannot be written rather than having to be answered with a null.
 *
 * <p>Both kinds answer the four things a contest is read for, which is what lets a ranking, a
 * count of colonies or a listing of factions walk one list rather than two. Only a reader that has
 * to explain the arithmetic behind a score routes on the kind, and the seal makes that routing
 * exhaustive.
 *
 * <p>A presence-only standing can never win a system: it scores nought, and the mechanic takes the
 * lead only on a score strictly greater than nought. Carrying one therefore widens what a contest
 * reports without moving any claimant.
 */
public sealed interface FactionClaimStanding
    permits WeighedClaimStanding, PresenceOnlyClaimStanding {

    /**
     * @return the id of the faction this standing belongs to
     */
    String factionId();

    /**
     * @return whether the faction's punitive-expedition data marks it territorial - the gate a
     *         faction must pass before any score can claim a system
     */
    boolean isTerritorial();

    /**
     * @return what the contest weighs the faction's presence at
     */
    int score();

    /**
     * Every market the faction holds in the system, in the order the system's listing reaches them
     * - the economy's own first, in economy order, then what the economy does not list.
     *
     * <p>One list rather than a market-it-stands-on plus the rest, so a reader counting or naming
     * a faction's colonies neither has to know which kind of standing it holds nor to remember to
     * consult a second accessor. A reader that does care which market carried the score asks the
     * weighed kind for it.
     *
     * @return the faction's markets in the system, in listing order
     */
    List<MarketClaimBreakdown> readHeldMarkets();
}
