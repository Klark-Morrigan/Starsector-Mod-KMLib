package kmlib.testfixtures.starsector.systems.claims;

import kmlib.starsector.entities.EntityNameplate;
import kmlib.starsector.systems.claims.ContestAdmission;
import kmlib.starsector.systems.claims.MarketClaimBreakdown;
import kmlib.starsector.systems.claims.PresenceOnlyClaimStanding;
import kmlib.starsector.systems.claims.WeighedClaimStanding;

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
 *
 * <p>Both kinds of standing are built here, since a case posing a contest usually wants the
 * distinction between them and nothing else about either - who was weighed, and who was merely
 * there.
 */
public final class ClaimStandingFixture {

    // The one colony a plainly-scored standing rests on. Named rather than left blank so a box
    // rendering the market list has something to print, and marked with nothing: a test posing
    // standings by score alone is not about how a market line is identified, and a glyph here would
    // put a mark on every box built from this fixture.
    private static final EntityNameplate STANDING_MARKET =
        EntityNameplate.createUnmarkedNameplate("Standing Colony");

    // The one colony a presence-only standing is present through, named apart from the scored one
    // so a box listing both kinds can be read line by line.
    private static final EntityNameplate UNWEIGHED_MARKET =
        EntityNameplate.createUnmarkedNameplate("Unweighed Colony");

    // What a colony the contest never weighed is worth. Its size is never read - the standing
    // reports a nought of its own - so it is stated once here rather than invented per case.
    private static final int UNWEIGHED_MARKET_SIZE = 3;

    // The market carries the whole score on its size, which is the shape with the fewest moving
    // parts: no siblings beside it and no garrison bonus folded into it.
    private static final int NO_SIBLING_MARKETS = 0;

    // Where the market falls in the system's listing. A test posing standings by score alone is
    // not posing a tie, so every standing built here takes the head of the list.
    private static final int FIRST_LISTED = 1;

    // The market is one the player has found. A test posing standings by score alone is posing no
    // fog-of-war case.
    private static final boolean IS_KNOWN_TO_PLAYER = true;

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
    public static WeighedClaimStanding buildStandingOnOneMarket(
            String factionId,
            int score,
            boolean isTerritorial) {

        return new WeighedClaimStanding(
            factionId,
            isTerritorial,
            new MarketClaimBreakdown(
                STANDING_MARKET,
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

    /**
     * A faction present in a system through one colony the contest never weighed, and so standing
     * at nought however large that colony is.
     *
     * <p>Takes no score, unlike its scored counterpart: a presence-only standing reports a nought
     * of its own, and a builder offering one to state would be offering a number that goes
     * nowhere.
     *
     * @param factionId     the faction the standing belongs to
     * @param isTerritorial whether the faction may claim a system at all - carried, and claiming
     *                      nothing here, since a nought passes no gate either way
     * @return the standing
     */
    public static PresenceOnlyClaimStanding buildPresenceOnlyStanding(
            String factionId,
            boolean isTerritorial) {

        return new PresenceOnlyClaimStanding(
            factionId,
            isTerritorial,
            List.of(new MarketClaimBreakdown(
                UNWEIGHED_MARKET,
                FIRST_LISTED,
                IS_KNOWN_TO_PLAYER,
                // Concealment rather than an absence from the economy's listing, arbitrarily: the
                // two suppress scoring identically, and a test posing a faction that was never
                // weighed is not about which of them did it.
                ContestAdmission.HIDDEN,
                UNWEIGHED_MARKET_SIZE,
                NO_SIBLING_MARKETS,
                OptionalInt.empty())));
    }
}
