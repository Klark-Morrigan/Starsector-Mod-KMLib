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

    // Where the market falls in the system's listing. A test posing standings by score alone is
    // not posing a tie, so every standing built here takes the head of the list.
    private static final int FIRST_LISTED = 1;

    // The market is one the player has found, held in the open. A test posing standings by score
    // alone is posing neither a fog-of-war case nor a hidden base - and a standing market could
    // not be hidden in any case, the mechanic never letting one stand for its faction.
    private static final boolean IS_KNOWN_TO_PLAYER = true;
    private static final boolean IS_NOT_HIDDEN = false;

    // The market is one the economy lists. A standing market could not be otherwise in any case,
    // the mechanic never reaching a colony the economy leaves out.
    private static final boolean IS_NOT_OFF_ECONOMY = false;

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
                FIRST_LISTED,
                IS_KNOWN_TO_PLAYER,
                IS_NOT_HIDDEN,
                IS_NOT_OFF_ECONOMY,
                score,
                NO_SIBLING_MARKETS,
                OptionalInt.empty()),
            List.of());
    }
}
