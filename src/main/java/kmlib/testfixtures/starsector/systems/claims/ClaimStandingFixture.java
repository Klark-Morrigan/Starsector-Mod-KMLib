package kmlib.testfixtures.starsector.systems.claims;

import kmlib.starsector.systems.claims.FactionClaimScore;
import kmlib.starsector.systems.claims.MarketClaimBreakdown;

import java.util.List;
import java.util.OptionalInt;

/**
 * Builds the standings a claim contest is posed with, for tests about what a contest means
 * rather than about the arithmetic behind it. Shipped from KMLib so both KMLib's and consuming
 * mods' tests state a standing the same way.
 *
 * <p>A standing is a market breakdown plus the faction it belongs to, so a test that cares only
 * about a score would otherwise have to invent a colony to carry it - and every such test would
 * invent a slightly different one. Stating the score directly keeps those cases about the score.
 */
public final class ClaimStandingFixture {

    // The one colony a plainly-scored standing rests on. Named rather than left blank so a box
    // rendering the market list has something to print.
    private static final String STANDING_MARKET_NAME = "Standing Colony";

    // The market carries the whole score on its size, which is the shape with the fewest moving
    // parts: no siblings beside it and no garrison bonus folded into it.
    private static final int NO_SIBLING_MARKETS = 0;

    private ClaimStandingFixture() {
    }

    /**
     * A faction standing on a single market worth exactly the given score and holding nothing
     * else in the system.
     *
     * @param factionId     the faction the standing belongs to
     * @param score         what the contest weighs the faction's presence at
     * @param isTerritorial whether the faction may claim a system at all
     * @return the standing
     */
    public static FactionClaimScore buildStandingOnOneMarket(
            String factionId,
            int score,
            boolean isTerritorial) {

        return new FactionClaimScore(
            factionId,
            isTerritorial,
            new MarketClaimBreakdown(
                STANDING_MARKET_NAME,
                score,
                NO_SIBLING_MARKETS,
                OptionalInt.empty()),
            List.of());
    }
}
