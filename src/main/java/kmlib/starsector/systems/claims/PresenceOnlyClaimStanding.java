package kmlib.starsector.systems.claims;

import java.util.List;

/**
 * One faction's place in a star system's claim contest where the mechanic weighed nothing for it:
 * every colony it holds there is one the walk skipped or never reached, so it is present without
 * ever having competed.
 *
 * <p>Two kinds of colony put a faction here, and they are the two the contest carries without
 * weighing: one held in concealment, which the walk skips before scoring, and one the economy does
 * not list, which the walk never reaches at all. A faction holding nothing else in the system has
 * no market that could stand for it, and this is what it gets instead of no standing at all - the
 * alternative being a system whose account names nobody while the map plainly draws a station in
 * that faction's colours.
 *
 * <p>The score is a named nought rather than the largest of the markets behind it. The contest
 * genuinely weighed the faction at nothing, and printing what its colonies would have been worth
 * would put a faction that took no part above the one that took the system. It also settles the
 * mechanic question outright: the lead changes only on a score strictly greater than nought, so
 * such a standing can neither win a system, tie for one, nor displace whoever holds it.
 *
 * <p>Territoriality is carried all the same, read off the faction the way a weighed standing reads
 * it. It says what the faction is rather than what it did here, and a reader is free to want that
 * about a faction that scored nothing.
 *
 * @param factionId        the ID of the faction this standing belongs to
 * @param isTerritorial    whether the faction's punitive-expedition data marks it territorial.
 *                         True is perfectly ordinary here - a territorial faction's only colony in
 *                         a system may well be a concealed base - and claims nothing either way,
 *                         the gate being one a score has to pass and this score being nought
 * @param unweighedMarkets every colony the faction holds in the system, in the order the listing
 *                         reaches them. Never empty in practice, a faction with no market at all
 *                         having no presence to stand for
 */
public record PresenceOnlyClaimStanding(
    String factionId,
    boolean isTerritorial,
    List<MarketClaimBreakdown> unweighedMarkets) implements FactionClaimStanding {

    // What the contest weighed this faction at. Nought rather than absent, because the faction is
    // on the list and the number states what its presence came to - and because the mechanic's own
    // strictly-greater comparison is what makes nought unable to take, tie or move a claim.
    private static final int NO_SCORE = 0;

    /**
     * Takes an immutable copy of the markets, and reads a null list as an empty one, so a standing
     * handed around a render pass cannot change under its readers.
     */
    public PresenceOnlyClaimStanding {
        unweighedMarkets = unweighedMarkets == null ? List.of() : List.copyOf(unweighedMarkets);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Nought however large the colonies behind it: the mechanic never weighed any of them, so
     * there is no sum to report and the size of what is present says nothing about the contest.
     */
    @Override
    public int score() {
        return NO_SCORE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The markets as carried, the listing order they were collected in being the order asked
     * for.
     */
    @Override
    public List<MarketClaimBreakdown> readHeldMarkets() {
        return unweighedMarkets;
    }
}
