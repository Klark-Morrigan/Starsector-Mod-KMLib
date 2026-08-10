package kmlib.testfixtures.starsector.systems.claims;

import kmlib.starsector.entities.EntityMapIcon;
import kmlib.starsector.systems.claims.ContestAdmission;
import kmlib.starsector.systems.claims.FactionClaimScore;
import kmlib.starsector.systems.claims.MarketClaimBreakdown;

import java.util.List;
import java.util.Optional;
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

    // The market is one the player has found. A test posing standings by score alone is posing no
    // fog-of-war case.
    private static final boolean IS_KNOWN_TO_PLAYER = true;

    // The colony's entity carries no map glyph. A test posing standings by score alone is not about
    // how a market line is identified, and an icon here would put a mark on every box built from
    // this fixture.
    private static final Optional<EntityMapIcon> NO_MAP_ICON = Optional.empty();

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
                NO_MAP_ICON,
                FIRST_LISTED,
                IS_KNOWN_TO_PLAYER,
                // Held in the open and listed by the economy, which a standing market could not be
                // otherwise in any case: the mechanic lets neither kind stand for a faction.
                ContestAdmission.WEIGHED,
                score,
                NO_SIBLING_MARKETS,
                OptionalInt.empty()),
            List.of());
    }
}
