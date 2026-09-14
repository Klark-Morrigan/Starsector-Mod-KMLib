package kmlib.starsector.systems.claims;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One faction's standing in a star system's claim contest where the mechanic weighed a market for
 * it: the market its presence there rests on, the rest of what it holds in the system, and whether
 * that presence is the kind that can take a claim at all.
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
 * @param factionId      the ID of the faction this standing belongs to
 * @param isTerritorial  whether the faction's punitive-expedition data marks it territorial -
 *                       the gate a faction must pass before any score can claim a system
 * @param standingMarket the faction's strongest scoring market in the system, the one its
 *                       whole standing rests on
 * @param otherMarkets   every other market the faction holds in the system, in the order the
 *                       listing reaches them. A market the contest never weighed is among them -
 *                       a hidden one, present for the sibling count, or one the economy does not
 *                       list, present on the map - though neither can be the standing market
 */
public record WeighedClaimStanding(
    String factionId,
    boolean isTerritorial,
    MarketClaimBreakdown standingMarket,
    List<MarketClaimBreakdown> otherMarkets) implements FactionClaimStanding {

    // The one market the standing itself rests on, which the faction's holdings run to beyond
    // whatever sits beside it. Named because it sizes a list rather than counting anything.
    private static final int THE_STANDING_MARKET = 1;

    /**
     * Takes an immutable copy of the other markets, and reads a null list as an empty one, so a
     * standing handed around a render pass cannot change under its readers.
     */
    public WeighedClaimStanding {
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
    @Override
    public int score() {
        return standingMarket.computeTotalScore();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Merged back into listing order rather than led by the standing market, because the split
     * between the two components is about which market carried the score and this read is about
     * what the faction holds. The listing place is what orders both parts, so re-sorting on it
     * restores the one order the walk ever imposed.
     */
    @Override
    public List<MarketClaimBreakdown> readHeldMarkets() {

        var heldMarkets =
            new ArrayList<MarketClaimBreakdown>(otherMarkets.size() + THE_STANDING_MARKET);

        heldMarkets.add(standingMarket);
        heldMarkets.addAll(otherMarkets);
        heldMarkets.sort(Comparator.comparingInt(MarketClaimBreakdown::listingPosition));

        return List.copyOf(heldMarkets);
    }
}
