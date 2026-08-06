package kmlib.starsector.systems.claims;

import java.util.List;

/**
 * One faction's standing in a star system's claim contest: the market its presence there rests
 * on, the rest of what it holds in the system, and whether that presence is the kind that can
 * take a claim at all.
 *
 * <p>The mechanic never sums a faction's holdings - it picks a single strongest market - so the
 * standing is that one market, measured the way the mechanic measures it, and two factions'
 * standings are directly comparable. The faction's other markets are carried all the same,
 * because the sibling term the standing is partly made of is exactly how many of them there
 * are: listed beside the number, it can be checked rather than taken on trust.
 *
 * <p>A faction that is not territorial is scored all the same. It can never win the system,
 * but it is present and it is competing for the space, and a reader asking who holds a system
 * is better served by seeing that than by seeing it silently dropped.
 *
 * @param factionId      the id of the faction this standing belongs to
 * @param isTerritorial  whether the faction's punitive-expedition data marks it territorial -
 *                       the gate a faction must pass before any score can claim a system
 * @param standingMarket the faction's strongest scoring market in the system, the one its
 *                       whole standing rests on
 * @param otherMarkets   every other market the faction holds in the system, in the order the
 *                       economy lists them. A hidden market is among them - it is present for
 *                       the sibling count - though it can never be the standing market itself
 */
public record FactionClaimScore(
    String factionId,
    boolean isTerritorial,
    MarketClaimBreakdown standingMarket,
    List<MarketClaimBreakdown> otherMarkets) {

    /**
     * Takes an immutable copy of the other markets, and reads a null list as an empty one, so a
     * standing handed around a render pass cannot change under its readers.
     */
    public FactionClaimScore {
        otherMarkets = otherMarkets == null ? List.of() : List.copyOf(otherMarkets);
    }

    /**
     * The faction's score in the contest: what its standing market is worth.
     *
     * <p>Derived rather than stored, so it cannot drift from the terms printed beneath it - a
     * stored score would be a second copy of the same sum, free to disagree with its parts.
     *
     * @return the standing market's total claim score
     */
    public int score() {
        return standingMarket.computeTotalScore();
    }
}
